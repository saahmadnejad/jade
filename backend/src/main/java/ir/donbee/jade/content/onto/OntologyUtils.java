package ir.donbee.jade.content.onto;

//#J2ME_EXCLUDE_FILE
//#APIDOC_EXCLUDE_FILE

import ir.donbee.jade.content.onto.Ontology;
import ir.donbee.jade.content.onto.OntologyException;

public class OntologyUtils {

	public static void exploreOntology(Ontology ontology) throws OntologyException {
		ontology.dump();
	}
}
