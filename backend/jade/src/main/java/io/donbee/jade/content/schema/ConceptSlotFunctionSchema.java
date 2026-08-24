package io.donbee.jade.content.schema;

import io.donbee.jade.content.abs.AbsConceptSlotFunction;
import io.donbee.jade.content.abs.AbsObject;
import io.donbee.jade.content.onto.OntologyException;

//#APIDOC_EXCLUDE_FILE

public class ConceptSlotFunctionSchema extends ConceptSchema {
	public static final String CONCEPT_SLOT_FUNCTION_CONCEPT = "CSF_CONCEPT";
	
	public ConceptSlotFunctionSchema(String slotName) {
		super(slotName);
		
		add(CONCEPT_SLOT_FUNCTION_CONCEPT, ConceptSchema.getBaseSchema());
		
		this.setEncodingByOrder(true);
	}
	
	public AbsObject newInstance() throws OntologyException {
		return new AbsConceptSlotFunction(getTypeName());
	} 
}
