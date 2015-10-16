package org.example.feature;

import org.eclipse.bpmn2.modeler.core.features.BendpointConnectionRouter;
import org.eclipse.bpmn2.modeler.core.features.ConnectionFeatureContainer;
import org.eclipse.bpmn2.modeler.core.utils.AnchorUtil;
import org.eclipse.graphiti.features.IFeatureProvider;
import org.eclipse.graphiti.features.context.IContext;
import org.eclipse.graphiti.features.context.IMoveBendpointContext;
import org.eclipse.graphiti.features.context.impl.LayoutContext;
import org.eclipse.graphiti.features.impl.DefaultMoveBendpointFeature;
import org.eclipse.graphiti.mm.pictograms.FreeFormConnection;
import org.eclipse.graphiti.mm.pictograms.Shape;


public class MoveBendPointFeature extends DefaultMoveBendpointFeature {

	public MoveBendPointFeature(IFeatureProvider fp) {
		super(fp);

	}

	@Override
	public boolean canExecute(IContext context) {
		return super.canExecute(context);
	}


	@Override
	public boolean moveBendpoint(IMoveBendpointContext context) {
		boolean moved = super.moveBendpoint(context);
		try {
			FreeFormConnection connection = context.getConnection();
			Shape connectionPointShape = AnchorUtil.getConnectionPointAt(connection, context.getBendpoint());
			if (connectionPointShape != null)
				AnchorUtil.setConnectionPointLocation(connectionPointShape, context.getX(), context.getY());

			BendpointConnectionRouter.setMovedBendpoint(connection, context.getBendpointIndex());
			new LayoutConnectionFeature(getFeatureProvider()).layout(new LayoutContext(connection));


		} catch (Exception e) {
			e.printStackTrace();
		}
		return moved;
	}


}
