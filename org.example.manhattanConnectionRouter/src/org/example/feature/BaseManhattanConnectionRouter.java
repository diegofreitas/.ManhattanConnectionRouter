/*******************************************************************************
 * Copyright (c) offset11, offset12 Red Hat, Inc.
 *  All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-voffset.html
 *
 * Contributors:
 * Red Hat, Inc. - initial API and implementation
 *
 * @author Bob Brodt
 ******************************************************************************/
package org.example.feature;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.eclipse.bpmn2.modeler.core.di.DIUtils;
import org.eclipse.bpmn2.modeler.core.features.BendpointConnectionRouter;
import org.eclipse.bpmn2.modeler.core.features.ConnectionRoute;
import org.eclipse.bpmn2.modeler.core.features.DetourPoints;
import org.eclipse.bpmn2.modeler.core.features.RouteSolver;
import org.eclipse.bpmn2.modeler.core.utils.AnchorUtil;
import org.eclipse.bpmn2.modeler.core.utils.AnchorUtil.AnchorLocation;
import org.eclipse.bpmn2.modeler.core.utils.AnchorUtil.BoundaryAnchor;
import org.eclipse.bpmn2.modeler.core.utils.GraphicsUtil;
import org.eclipse.bpmn2.modeler.core.utils.GraphicsUtil.LineSegment;
import org.eclipse.graphiti.features.IFeatureProvider;
import org.eclipse.graphiti.mm.algorithms.styles.Point;
import org.eclipse.graphiti.mm.pictograms.Anchor;
import org.eclipse.graphiti.mm.pictograms.Connection;
import org.eclipse.graphiti.mm.pictograms.ContainerShape;
import org.eclipse.graphiti.mm.pictograms.FixPointAnchor;
import org.eclipse.graphiti.mm.pictograms.Shape;
import org.eclipse.graphiti.services.Graphiti;


/**
 * A Connection Router that constrains all line segments of a connection to be either
 * horizontal or vertical; thus, diagonal lines are split into two segments that are
 * horizontal and vertical.
 * 
 * This is a final class because it needs to ensure the routing info for
 * the connection is cleaned up when it's done, so we don't want to allow
 * this class to be subclassed.
 */
public class BaseManhattanConnectionRouter extends BendpointConnectionRouter {

	protected LineSegment sourceTopEdge;
	protected LineSegment sourceBottomEdge;
	protected LineSegment sourceLeftEdge;
	protected LineSegment sourceRightEdge;

	protected LineSegment targetTopEdge;
	protected LineSegment targetBottomEdge;
	protected LineSegment targetLeftEdge;
	protected LineSegment targetRightEdge;
	
	static final int offset = 15;
	static boolean testRouteSolver = false;
	
	enum Orientation {
		HORIZONTAL, VERTICAL, NONE
	};
	
	public BaseManhattanConnectionRouter(IFeatureProvider fp) {
		super(fp);
	}
	
	
	@Override
	protected ConnectionRoute calculateRoute() {
		
		if (isSelfConnection())
			return super.calculateRoute();
		
		
		
		// The list of all possible routes. The shortest will be used.
		List<ConnectionRoute> allRoutes = new ArrayList<ConnectionRoute>();

		List <BoundaryAnchor> sourceBoundaryAnchors = getBoundaryAnchors(source);
		List <BoundaryAnchor>  targetBoundaryAnchors = getBoundaryAnchors(target);
		
		sourceAnchor = this.connection.getStart();
		targetAnchor = this.connection.getEnd();
		
		Point start;
		Point end;
		Point middle = getSegmentPoints();


		if (testRouteSolver) {
			findAllShapes();
			RouteSolver solver = new RouteSolver(fp, allShapes);
			if (solver.solve(source, target))
				return null;
		}
		
		
		
		
		
		if (sourceAnchor!=null) {
			// use ad-hoc anchor for source:
			// the connection's source location will remain fixed.
			start = GraphicsUtil.createPoint(sourceAnchor);
			if (targetAnchor!=null) {
				// use ad-hoc anchor for target:
				// the connection's target location will also remain fixed
				end = GraphicsUtil.createPoint(targetAnchor);
				calculateRoute(allRoutes, source,start,middle,target,end, Orientation.HORIZONTAL);
				calculateRoute(allRoutes, source,start,middle,target,end, Orientation.VERTICAL);
			}
			else {
				// use boundary anchors for target:
				// calculate 4 possible routes to the target,
				// ending at each of the 4 boundary anchors
				for (BoundaryAnchor targetEntry : targetBoundaryAnchors) {
					end = GraphicsUtil.createPoint(targetEntry.anchor);

					calculateRoute(allRoutes, source,start,middle,target,end, Orientation.HORIZONTAL);
					calculateRoute(allRoutes, source,start,middle,target,end, Orientation.VERTICAL);
				}
			}
		}
		else {
			// use boundary anchors for source:
			// calculate 4 possible routes from the source,
			// starting at each of the 4 boundary anchors

			for (BoundaryAnchor sourceEntry : sourceBoundaryAnchors) {
				start = GraphicsUtil.createPoint(sourceEntry.anchor);

				if (targetAnchor!=null) {
					// use ad-hoc anchor for target:
					// the connection's target location will also remain fixed
					end = GraphicsUtil.createPoint(targetAnchor);
					calculateRoute(allRoutes, source,start,middle,target,end, Orientation.HORIZONTAL);
					calculateRoute(allRoutes, source,start,middle,target,end, Orientation.VERTICAL);
				}
				else {
					// use boundary anchors for target:
					// calculate 4 possible routes to the target,
					// ending at each of the 4 boundary anchors

					for (BoundaryAnchor targetEntry : targetBoundaryAnchors ) {
						end = GraphicsUtil.createPoint(targetEntry.anchor);

						calculateRoute(allRoutes, source,start,middle,target,end, Orientation.HORIZONTAL);
						calculateRoute(allRoutes, source,start,middle,target,end, Orientation.VERTICAL);
					}
				}
			}
		}
		
		// pick the shortest route
		ConnectionRoute route = null;
		if (allRoutes.size()==1) {
			route = allRoutes.get(0);
			GraphicsUtil.dump("Only one valid route: "+route.toString());
		}
		else if (allRoutes.size()>1) {
			GraphicsUtil.dump("Optimizing Routes:\n------------------");
			int delta = 5;
			int rank = allRoutes.size();
			for (ConnectionRoute r : allRoutes) {
				r.optimize();
				for (int i=0; i<r.size()-1; ++i) {
					if (GraphicsUtil.intersectsLine(source, r.get(i), r.get(i+1))) {
						r.setRank(rank);
						break;
					}
					if (GraphicsUtil.intersectsLine(target, r.get(i), r.get(i+1))) {
						r.setRank(rank);
						break;
					}
					if (GraphicsUtil.isSlanted(r.get(i),r.get(i+1))) {
						r.setRank(rank);
						break;
					}
				}
				AnchorLocation al = AnchorUtil.getBoundaryAnchorLocation(this.sourceAnchor);
				if (al==AnchorLocation.LEFT || al==AnchorLocation.RIGHT) {
					if (Math.abs(r.get(0).getX() - r.get(1).getX()) <= delta)
						r.setRank(rank/2);
				}
				else {
					if (Math.abs(r.get(0).getY() - r.get(1).getY()) <= delta)
						r.setRank(rank/2);
				}
				al = AnchorUtil.getBoundaryAnchorLocation(this.targetAnchor);
				if (al==AnchorLocation.LEFT || al==AnchorLocation.RIGHT) {
					if (Math.abs(r.get( r.size()-2 ).getX() - r.get( r.size()-1 ).getX()) <= delta)
						r.setRank(rank/2);
				}
				else {
					if (Math.abs(r.get( r.size()-2 ).getY() - r.get( r.size()-1 ).getY()) <= delta)
						r.setRank(rank/2);
				}
			}

			GraphicsUtil.dump("Calculating Crossings:\n------------------");
			// Connection crossings only participate in determining the best route,
			// we don't actually try to correct a route crossing a connection.
			for (ConnectionRoute r : allRoutes) {
				if (r.getPoints().size()>1) {
					Point p1 = r.getPoints().get(0);
					for (int i=1; i<r.getPoints().size(); ++i) {
						Point p2 = r.getPoints().get(i);
						List<Connection> crossings = findCrossings(p1, p2);
						for (Connection c : crossings) {
							if (c!=this.connection)
								r.addCrossing(c, p1, p2);
						}
						ContainerShape shape = getCollision(p1, p2);
						if (shape!=null) {
							r.addCollision(shape, p1, p2);
						}
						
						p1 = p2;
					}

				}
			}

			GraphicsUtil.dump("Sorting Routes:\n------------------");
			Collections.sort(allRoutes);
			
			drawConnectionRoutes(allRoutes);

			route = allRoutes.get(0);
		}
		
		return route;
	}
	

	@Override
	protected ContainerShape getCollision(Point p1, Point p2) {
		return super.getCollision(p1, p2);
	}
	
	@Override
	protected List<Connection> findCrossings(Point start, Point end) {
		return super.findCrossings(start, end);
	}

	protected Point getSegmentPoints() {
		LineSegment sourceEdges[] = GraphicsUtil.getEdges(source);
		sourceTopEdge = sourceEdges[0];
		sourceBottomEdge = sourceEdges[1];
		sourceLeftEdge = sourceEdges[2];
		sourceRightEdge = sourceEdges[3];

		LineSegment targetEdges[] = GraphicsUtil.getEdges(target);
		targetTopEdge = targetEdges[0];
		targetBottomEdge = targetEdges[1];
		targetLeftEdge = targetEdges[2];
		targetRightEdge = targetEdges[3];
		
		Point middle = null;
		if (movedBendpoint!=null) {
			middle = movedBendpoint;
			findAllShapes();
			for (ContainerShape shape : allShapes) {
				if (GraphicsUtil.contains(shape, middle)) {
					middle = null;
					break;
				}
			}
		}
		return middle;
	}
	
	@Override
	protected List<ContainerShape> findAllShapes() {
		return super.findAllShapes();
	}
	

	protected ConnectionRoute calculateRoute(List<ConnectionRoute> allRoutes, Shape source, Point start, Point middle, Shape target, Point end, Orientation orientation) {
		
		ConnectionRoute route = new ConnectionRoute(this, allRoutes.size()+1, source,target);

		if (middle!=null) {
			List<Point> departure = calculateDeparture(source, start, middle);
			List<Point> approach = calculateApproach(middle, target, end);

			route.getPoints().addAll(departure);
			calculateEnroute(route, departure.get(departure.size()-1), middle, orientation);
			route.getPoints().add(middle);
			calculateEnroute(route, middle,approach.get(0),orientation);
			route.getPoints().addAll(approach);
		}
		else {
			List<Point> departure = calculateDeparture(source, start, end);
			List<Point> approach = calculateApproach(start, target, end);
			route.getPoints().addAll(departure);
			calculateEnroute(route, departure.get(departure.size()-1), approach.get(0), orientation);
			route.getPoints().addAll(approach);
		}
		
		if (route.isValid())
			allRoutes.add(route);
		
		return route;
	}
	
	private Point getVertMidpoint(Point start, Point end, double fract) {
		Point m = GraphicsUtil.createPoint(start);
		int d = (int)(fract * (double)(end.getY() - start.getY()));
		m.setY(start.getY()+d);
		return m;
	}
	
	private Point getHorzMidpoint(Point start, Point end, double fract) {
		Point m = GraphicsUtil.createPoint(start);
		int d = (int)(fract * (double)(end.getX() - start.getX()));
		m.setX(start.getX()+d);
		return m;
	}

	protected List<Point> calculateDeparture(Shape source, Point start, Point end) {
		AnchorLocation sourceEdge = AnchorUtil.getBoundaryAnchorLocation(sourceAnchor);
		List<Point> points = new ArrayList<Point>();
		
		Point p = GraphicsUtil.createPoint(start);
		Point m = end;
		
		switch (sourceEdge) {
		case TOP:
		case BOTTOM:
			for (;;) {
				m = getVertMidpoint(start,m,0.45);
				ContainerShape shape = getCollision(start,m);
				
				if (shape==null || Math.abs(m.getY()-start.getY())<=offset)
					break;
			}
			p.setY( m.getY() );
			break;
		case LEFT:
		case RIGHT:
			for (;;) {
				m = getHorzMidpoint(start,m,0.45);
				ContainerShape shape = getCollision(start,m);
				if (shape==null || Math.abs(m.getX()-start.getX())<=offset)
					break;
			}
			p.setX( m.getX() );
			break;
		default:
			return points;
		}
		
		points.add(start);
		points.add(p);
		
		return points;
	}
	
	protected List<Point> calculateApproach(Point start, Shape target, Point end) {
		AnchorLocation targetEdge = AnchorUtil.getBoundaryAnchorLocation(targetAnchor);
		List<Point> points = new ArrayList<Point>();
		
		Point p = GraphicsUtil.createPoint(end);
		Point m = start;
		switch (targetEdge) {
		case TOP:
		case BOTTOM:
			for (;;) {
				m = getVertMidpoint(m,end,0.45);
				ContainerShape shape = getCollision(m,end);
				if (shape==null || shape==target || Math.abs(m.getY()-end.getY())<=offset)
					break;
			}
			p.setY( m.getY() );
			break;
		case LEFT:
			if(start.getX()>end.getX()){
				p.setX(end.getX() - 2 * offset);
				break;
			}
		case RIGHT:
			if(start.getX()<end.getX() &&  targetEdge == AnchorLocation.RIGHT){
				p.setX(end.getX() + 2 * offset);
				break;
			}
			
			for (;;) {
				m = getHorzMidpoint(m,end,0.45);
				ContainerShape shape = getCollision(m,end);
				if (shape==null || shape==target || Math.abs(m.getX()-end.getX())<=offset)
					break;
			}
			p.setX( m.getX() );
			break;
		default:
			return points;
		}
		
		points.add(p);
		points.add(end);
		
		return points;
	}

	Point createPoint(int x, int y) {
		return GraphicsUtil.createPoint(x, y); 
	}
	
	protected boolean calculateEnroute(ConnectionRoute route, Point start, Point end, Orientation orientation) {
		if (GraphicsUtil.pointsEqual(start, end))
			return false;
		
		Point p;
		
		// special case: if start and end can be connected with a horizontal or vertical line
		// check if there's a collision in the way. If so, we need to navigate around it.
		if (!GraphicsUtil.isSlanted(start,end)) {
			ContainerShape shape = getCollision(start,end);
			if (shape==null) {
				return true;
			}
		}



		int dx = Math.abs(end.getX() - start.getX());
		int dy = Math.abs(end.getY() - start.getY());
		if (orientation==Orientation.NONE) {
			if (dx>dy) {
				orientation = Orientation.HORIZONTAL;
			}
			else {
				orientation = Orientation.VERTICAL;
			}
		}
		
		if (orientation == Orientation.HORIZONTAL) {
			p = createPoint(end.getX(), start.getY());
			ContainerShape shape = getCollision(start,p);
			if (shape!=null) {


				DetourPoints detour = getDetourPoints(shape);
				// this should be a vertical segment - navigate around the shape
				// go up or down from here?
				boolean detourUp = (end.getY() - start.getY() > 0);
				int dyTop = Math.abs(p.getY() - detour.topLeft.getY());
				int dyBottom = Math.abs(p.getY() - detour.bottomLeft.getY());
				if (dy<dyTop || dy<dyBottom)
					detourUp = dyTop < dyBottom;

				if (p.getX() > start.getX()) {
					p.setX( detour.topLeft.getX() );
					route.add(p);
					if (detourUp) {
						route.add(detour.topLeft);
						route.add(detour.topRight);
					}
					else {
						route.add(detour.bottomLeft);
						route.add(detour.bottomRight);
					}

				}
				else {
					p.setX( detour.topRight.getX() );
					route.add(p);
					if (detourUp) {
						route.add(detour.topRight);
						route.add(detour.topLeft);
					}
					else {
						route.add(detour.bottomRight);
						route.add(detour.bottomLeft);
					}

				}
				p = route.get(route.size()-1);
			}
			else
				route.add(p);
		}
		else {
			p = createPoint(start.getX(), end.getY());
			ContainerShape shape = getCollision(start,p);
			if (shape!=null) {

				DetourPoints detour = getDetourPoints(shape);
				// this should be a horizontal segment - navigate around the shape
				// go left or right from here?
				boolean detourLeft = end.getX() - start.getX() < 0;
				int dxLeft = Math.abs(p.getX() - detour.topLeft.getX());
				int dxRight = Math.abs(p.getX() - detour.topRight.getX());
				if (dx<dxLeft || dx<dxRight)
					detourLeft = dxLeft < dxRight;

				if (p.getY() > start.getY()) {
					p.setY( detour.topLeft.getY() );
					route.add(p);
					if (detourLeft) {
						// go around to the left
						route.add(detour.topLeft);
						route.add(detour.bottomLeft);
					}
					else {
						// go around to the right
						route.add(detour.topRight);
						route.add(detour.bottomRight);
					}
				}
				else {
					p.setY( detour.bottomLeft.getY() );
					route.add(p);
					if (detourLeft) {
						route.add(detour.bottomLeft);
						route.add(detour.topLeft);
					}
					else {
						route.add(detour.bottomRight);
						route.add(detour.topRight);
					}

				}
				p = route.get(route.size()-1);
			}
			else
				route.add(p);
		}
		
		if (route.isValid())
			calculateEnroute(route,p,end,Orientation.NONE);
		
		return route.isValid();
	}
	

	protected DetourPoints getDetourPoints(ContainerShape shape) {
		DetourPoints detour = new DetourPoints(shape, offset);
		if (allShapes==null)
			findAllShapes();
		
		for (int i=0; i<allShapes.size(); ++i) {
			ContainerShape s = allShapes.get(i);
			if (shape==s)
				continue;
			DetourPoints d = new DetourPoints(s, offset);
			
			if (detour.intersects(d) && !detour.contains(d)) {
				detour.merge(d);
				i = -1;
			}
		}

		return detour;
	}
	
	protected void finalizeConnection() {
	}
	
	protected boolean fixCollisions() {
		return false;
	}
	
	protected boolean calculateAnchors() {
		return false;
	}
	protected void updateConnection() {
		DIUtils.updateDIEdge(ffc);
	}

	protected List <BoundaryAnchor> getBoundaryAnchors(Shape s) {
		List <BoundaryAnchor> anchorList= new ArrayList<BoundaryAnchor> ();
		Iterator<Anchor> iterator = s.getAnchors().iterator();
		while (iterator.hasNext()) {
			Anchor anchor = iterator.next();
			String property = Graphiti.getPeService().getPropertyValue(anchor, AnchorUtil.BOUNDARY_FIXPOINT_ANCHOR);
			if (property != null && anchor instanceof FixPointAnchor) {
				BoundaryAnchor a = new BoundaryAnchor();
				a.anchor = (FixPointAnchor) anchor;
				a.locationType = AnchorLocation.getLocation(property);
				a.location = peService.getLocationRelativeToDiagram(anchor);
				anchorList.add(a);
			}
		}
		return anchorList;
	}

	

	
}
