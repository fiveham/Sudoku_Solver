package sudoku;

public class ValueClaim extends Resolvable {
	
	private FactBag source, recipient;
	
	public ValueClaim(FactBag src, FactBag recipient) {
		this.source = src;
		this.recipient = recipient;
	}
	
	@Override
	public boolean resolve(){
		return recipient.collapseTo(source);
	}
	
}
