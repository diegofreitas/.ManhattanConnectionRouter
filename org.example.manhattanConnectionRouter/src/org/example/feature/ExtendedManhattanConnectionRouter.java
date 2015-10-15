package org.example.feature;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.bpmn2.modeler.core.utils.GraphicsUtil;
import org.eclipse.emf.common.util.TreeIterator;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.graphiti.features.IFeatureProvider;
import org.eclipse.graphiti.mm.pictograms.ContainerShape;
import org.eclipse.graphiti.mm.pictograms.Diagram;

public class ExtendedManhattanConnectionRouter extends BaseManhattanConnectionRouter {

	public ExtendedManhattanConnectionRouter(IFeatureProvider fp) {
		super(fp);
	}

	protected List<ContainerShape> findAllShapes() {
		allShapes = new ArrayList<ContainerShape>();
		Diagram diagram = fp.getDiagramTypeProvider().getDiagram();
		TreeIterator<EObject> iter = diagram.eAllContents();
		while (iter.hasNext()) {
			EObject o = iter.next();
			if (o instanceof ContainerShape) {

				ContainerShape shape = (ContainerShape)o;
				if (shape==source.eContainer() || shape==target.eContainer())
					continue;

				allShapes.add(shape);
			}
		}
		GraphicsUtil.dump("All Shapes", allShapes);
		return allShapes;
	}
}
