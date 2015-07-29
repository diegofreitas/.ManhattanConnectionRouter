package org.example.feature;

import org.eclipse.graphiti.mm.pictograms.FixPointAnchor;

//Isolating domain classes of my project :p
public interface AnchorVerifier {
	boolean isAnchorsFromSameConnection(FixPointAnchor sourceAnchor, FixPointAnchor targetAnchor);
}