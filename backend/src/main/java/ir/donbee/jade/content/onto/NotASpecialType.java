package ir.donbee.jade.content.onto;

//#APIDOC_EXCLUDE_FILE

public class NotASpecialType extends OntologyException {

	public NotASpecialType() {
		super("");
	}
	
    public Throwable fillInStackTrace() {
        return this;
    }
}
