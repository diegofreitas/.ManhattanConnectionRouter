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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.eclipse.bpmn2.modeler.core.features.BendpointConnectionRouter;
import org.eclipse.bpmn2.modeler.core.features.ConnectionRoute;
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

	private static final int A_STEP = 30;
	protected LineSegment sourceTopEdge;
	protected LineSegment sourceBottomEdge;
	protected LineSegment sourceLeftEdge;
	protected LineSegment sourceRightEdge;

	protected LineSegment targetTopEdge;
	protected LineSegment targetBottomEdge;
	protected LineSegment targetLeftEdge;
	protected LineSegment targetRightEdge;

	static final int offset = 10;
	static boolean testRouteSolver = false;
	

	public BaseManhattanConnectionRouter(IFeatureProvider fp, AnchorVerifier anchorVerifier) {
		super(fp);
		this.anchorVerifier = anchorVerifier;
	}

	private AnchorVerifier anchorVerifier;
	private Map<Coordinate, Integer> g_score;
	private Map<Coordinate, Integer> f_score;
	private Map<Coordinate, Coordinate> came_from;

	@Override
	protected ConnectionRoute calculateRoute() {
		if (isSelfConnection())
			return super.calculateRoute();

		// The list of all possible routes. The shortest will be used.
		List<ConnectionRoute> allRoutes = new ArrayList<ConnectionRoute>();
		
		sourceAnchor = this.connection.getStart();
		targetAnchor = this.connection.getEnd();

		
		Point startP;
		Point endP;

		startP = GraphicsUtil.createPoint(sourceAnchor);
		endP = GraphicsUtil.createPoint(targetAnchor);
		int startModifier = AnchorUtil.getBoundaryAnchorLocation(sourceAnchor).equals(AnchorLocation.LEFT) ? -20 : 20;
		int endModifier = AnchorUtil.getBoundaryAnchorLocation(targetAnchor).equals(AnchorLocation.LEFT) ? -20 : 20;
		Coordinate start = new Coordinate(startP.getX()+startModifier, startP.getY());
		Coordinate end = new Coordinate(endP.getX()+endModifier, endP.getY());
		ConnectionRoute route = new ConnectionRoute(this, allRoutes.size()+1, source,target);

		List<Coordinate> astarResult = aStar(start, end);
		List<Point> reducedAstar = calculateSegments(startP, astarResult, endP);
		route.getPoints().addAll(reducedAstar);
		allRoutes.add(route);
		
		drawConnectionRoutes(allRoutes);

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

	@Override
	protected List<ContainerShape> findAllShapes() {
		return super.findAllShapes();
	}

	protected List <BoundaryAnchor> getBoundaryAnchors(Shape s) {
		List <BoundaryAnchor> anchorList= new ArrayList<BoundaryAnchor> ();
		Iterator<Anchor> iterator = s.getAnchors().iterator();
		while (iterator.hasNext()) {
			Anchor anchor = iterator.next();
			String property = Graphiti.getPeService().getPropertyValue(anchor, AnchorUtil.BOUNDARY_FIXPOINT_ANCHOR);
			if (property != null && anchor instanceof FixPointAnchor) {
				BoundaryAnchor boundaryAnchor = new BoundaryAnchor();
				boundaryAnchor.anchor = (FixPointAnchor) anchor;
				boundaryAnchor.locationType = AnchorLocation.getLocation(property);
				boundaryAnchor.location = peService.getLocationRelativeToDiagram(anchor);
				anchorList.add(boundaryAnchor);
			}
		}
		return anchorList;
	}

	public List<Point> calculateSegments(Point start, List<Coordinate> points, Point end) {
		List<Point> result = new ArrayList<Point>();
		result.add(start);
		for (int i = points.size() - 1; i >= 0; i--) {
			Coordinate curr = points.get(i);
			if(i < 1 ){
				result.add(GraphicsUtil.createPoint(curr.x, curr.y));
				continue;
			}

			if( i > points.size() - 2){
				result.add(GraphicsUtil.createPoint(curr.x, curr.y));
				continue;
			}

			Coordinate prev = points.get(i+1);
			Coordinate next = points.get(i-1);
			if(prev.x==curr.x&&curr.x!=next.x) {
				result.add(GraphicsUtil.createPoint(curr.x, curr.y));
			}
			if(prev.y==curr.y&&curr.y!=next.y){
				result.add(GraphicsUtil.createPoint(curr.x, curr.y));
			}
		}
		result.add(end);
		
			
		return result;
	}
	
	public static class Coordinate {
		int x;
		int y;
		
		public Coordinate(int x, int y) {
			this.x = x;
			this.y = y;
		}

		@Override
		public String toString() {
			return "Coordinate [x=" + x + ", y=" + y + "]";
		}



		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + x;
			result = prime * result + y;
			return result;
		}
		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			Coordinate other = (Coordinate) obj;
			if (x != other.x )
				return false;
			if (y != other.y )
				return false;
			return true;
		}
	}

	//Based on https://en.wikipedia.org/wiki/A*_search_algorithm
	protected List<Coordinate> aStar(Coordinate start, Coordinate goal) {
		if(getCollision(GraphicsUtil.createPoint(start.x, start.y),
				GraphicsUtil.createPoint(start.x, start.y))!=null ||
				getCollision(GraphicsUtil.createPoint(goal.x, goal.y),
						GraphicsUtil.createPoint(goal.x, goal.y))!=null) {
			List<Coordinate> alterResult = new ArrayList<Coordinate>();
			alterResult.add(goal);
			alterResult.add(start);
			return alterResult;
		}
		
		
		Set<Coordinate> closedset = new HashSet<Coordinate>();
		Set<Coordinate> openset = new HashSet<Coordinate>();
		openset.add(start);
		came_from = new HashMap<Coordinate, Coordinate>();

		g_score = new HashMap<Coordinate, Integer>();
		g_score.put(start, 0);

		f_score = new HashMap<Coordinate, Integer>();
		Integer currentGScore = getGScore(start);
		f_score.put(start,currentGScore+heuristicCostEstimate(start, goal));

		while(!openset.isEmpty()) {
			Coordinate current = lowestFScore(f_score, openset);
			if(current.equals(goal)) {
				return reconstructPath(came_from, goal);
			}


			openset.remove(current);
			closedset.add(current);
			for(Coordinate neighbor:neighborNodes(current, goal)) {
				if(closedset.contains(neighbor)) continue;
				ContainerShape hadCollision = getCollision(GraphicsUtil.createPoint(neighbor.x, neighbor.y),
						GraphicsUtil.createPoint(neighbor.x, neighbor.y));
				
				if(hadCollision!=null) {
					closedset.add(neighbor);
					continue;
				}
				int gScoreValue= Integer.valueOf(getGScore(current));
				int tentative_g_score = gScoreValue + heuristicCostEstimate(current,neighbor);

				if(!openset.contains(neighbor) || tentative_g_score < getGScore(current)) {
					came_from.put(neighbor, current);
					g_score.put(neighbor, tentative_g_score);
					f_score.put(neighbor, tentative_g_score + heuristicCostEstimate(neighbor, goal));
					openset.add(neighbor);
				}
			}
		}

		List<Coordinate> alterResult = new ArrayList<Coordinate>();
		alterResult.add(start);
		alterResult.add(goal);
		return alterResult;
	}


	private Integer getGScore(Coordinate start) {
		Integer currentGScore = g_score.get(start);
		if(currentGScore == null) {
			currentGScore = Integer.MAX_VALUE;
		}
		else {
			Coordinate parent = came_from.get(start);
			if(parent!=null) {
				Coordinate ancestor = came_from.get(parent);
				if(ancestor!=null) {
					currentGScore += (parent.x == ancestor.x && start.x != parent.x)
							|| (parent.y == ancestor.y && start.y != parent.y) ? 12 : 0; // if the current node is a turn, make it weight more
				}
			}
		}
		return currentGScore;
	}

	public int heuristicCostEstimate(Coordinate a, Coordinate b) {
		return Math.abs(a.x-b.x)+Math.abs(a.y-b.y);
	}

	protected Coordinate lowestFScore(Map<Coordinate, Integer> f_score, Set<Coordinate>  openset) {
		Entry<Coordinate, Integer> min = null;
		for(Entry<Coordinate, Integer> entry : f_score.entrySet()) {
			if(openset.contains(entry.getKey()) && (min==null || min.getValue() > entry.getValue())) {
				min = entry;
			}
		}
		return min.getKey();
	}

	protected List<Coordinate> neighborNodes(Coordinate current, Coordinate goal) {
		List<Coordinate> list = new ArrayList<Coordinate>();
		int heuristicCostEstimate  = remainingDistance(current, goal);
		int step = heuristicCostEstimate > A_STEP ? A_STEP: heuristicCostEstimate;
		list.add(new Coordinate(current.x+step, current.y));
		if(current.x-step >= 0) list.add(new Coordinate(current.x-step, current.y));
		list.add(new Coordinate(current.x, current.y+step));
		if(current.y-step >= 0) list.add(new Coordinate(current.x, current.y-step));
		return list;
	}
	
	private int remainingDistance(Coordinate a, Coordinate b){
		if(a.x == b.x && a.y == b.y){
			return 0;
		} else if (a.x == b.x){
			return Math.abs(a.y - b.y);
		} else {
			return Math.abs(a.x - b.x);
		}
	}

	protected List<Coordinate> reconstructPath(Map<Coordinate, Coordinate> came_from, Coordinate current) {
		List<Coordinate> totalPath = new ArrayList<Coordinate>();
		totalPath.add(current);
		while(came_from.containsKey(current)) {
			current = came_from.get(current);
			totalPath.add(current);
		}
		return totalPath;
	}
}
