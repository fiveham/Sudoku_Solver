package sudoku;

import common.graph.Vertex;
import common.time.Time;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * <p>A set that's a node in {@link Puzzle a graph representation of a sudoku target}, 
 * such that the NodeSet itself is a collection of its {@link Vertex#neighbors() neighbors}.</p>
 * 
 * <p>The NodeSet class serves as a pool to unite certain operations performed identically 
 * by both Claim and Rule. Most importantly, NodeSet serves to enforce, in one 
 * consistent location, the rule that any node's neighbors must know that that node in 
 * question is one of their neighbors: neighbor status must be symmetrical. Additionally, 
 * NodeSet being superclass to both Claim and Rule enables Claim and Rule to be elements 
 * of the same collection, so that a bipartite graph like Puzzle doesn't need to be described 
 * as a bipartite graph explicitly via a BipartiteGraph contract, as that would feed into 
 * combinatorial explosion, given the need for WrappingGraphs as well.</p>
 * 
 * @author fiveham
 * @param <T> The type of the elements of this Set.
 * @param <S> The type of the Set elements of this Set. This should also be a proxy 
 * for this type itself.
 */
public class NodeSet<T extends NodeSet<S,T>, S extends NodeSet<T,S>> extends ToolSet<T> implements Vertex<NodeSet<?,?>>{
	/*
	 * TODO get a real solution for the lack of a context Time in regular Collection methods
	 * 
	 * Idea: Include a (real) Time stack in each NodeSet, and require external use of the time 
	 * stack by the calling context before and after calls to the standard Collection methods
	 */
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -5327679229184923974L;
	
	protected Puzzle puzzle;

	public NodeSet(Puzzle puzzle){
		this.puzzle = puzzle;
	}

	public NodeSet(Puzzle puzzle, Collection<T> c) {
		super(c);
		this.puzzle = puzzle;
	}

	public NodeSet(Puzzle puzzle, int initialCapacity) {
		super(initialCapacity);
		this.puzzle = puzzle;
	}

	public NodeSet(Puzzle puzzle, int initialCapacity, float loadFactor) {
		super(initialCapacity, loadFactor);
		this.puzzle = puzzle;
	}
	
	/**
	 * <p>Returns the target to which this NodeSet belongs.</p>
	 * @return the target to which this NodeSet belongs
	 */
	public Puzzle getPuzzle(){
		return puzzle;
	}
	
	@Override
	@SuppressWarnings("unchecked")
	public boolean add(T e){
		boolean result = super.add(e);
		if(result){
			e.add((S)this);
		}
		return result;
	}
	
	@Override
	public final boolean addAll(Collection<? extends T> c){
		boolean result = false;
		for(T t : c){
			result |= add(t);
		}
		return result;
	}
	
	@Override
	public final boolean remove(Object o){
		boolean result = remove_internal(o);
		
		if(result){
			validateFinalState(new DummyTime());
		}
		return result;
	}
	
	public final boolean remove(SolutionEvent time, Object o){
		boolean result = remove_internal(time, o);
		
		if(result){
			validateFinalState(time);
		}
		return result;
	}
	
	/**
	 * <p>Removes <tt>o</tt> and removes <tt>this</tt> from <tt>o</tt> 
	 * without {@link #validateFinalState() validating the set afterwards}.</p>
	 * 
	 * <p>Used internally so that bulk operations can validate the set's state 
	 * only after they have completed instead of numerous times throughout 
	 * the bulk operation and so that only one removal method needs to ensure 
	 * mutual element-removal. If bulk operations were subject to final-state 
	 * validation checks by calling remove(Object) repeatedly, then any bulk 
	 * removal operation that removes all but one of the elements of this set 
	 * could force a Rule to trigger a value-claim event in the middle of the 
	 * operation even though such a Rule really ought to wait until the end 
	 * to validate, which allows just one automatic resolution event to trigger.</p>
	 * 
	 * @param o the object being removed
	 * @return true if this set has been changed by the operation, false otherwise
	 */
	@SuppressWarnings("unchecked")
	private boolean remove_internal(Object o){
		boolean result = super.remove(o);
		if(result){
			((T)o).remove(this);
		}
		return result;
	}

	@SuppressWarnings("unchecked")
	private boolean remove_internal(SolutionEvent time, Object o){
		boolean result = super.remove(o);
		if(result){
			((T)o).remove(time, this);
		}
		return result;
	}
	
	@Override
	public final boolean removeAll(Collection<?> c){
		boolean result = false;
		
		for(Object o : c){
			result |= remove_internal(o);
		}
		if(result){
			validateFinalState(new DummyTime());
		}
		return result;
	}
	
	public final boolean removeAll(SolutionEvent time, Collection<?> c){
		boolean result = false;
		
		for(Object o : c){
			result |= remove_internal(time, o);
		}
		if(result){
			validateFinalState(time);
		}
		return result;
	}
	
	@Override
	public final boolean retainAll(Collection<?> c){
		boolean result = false;
		
		Iterator<T> iter = super.iterator();
		for(T t; iter.hasNext();){
			if(!c.contains(t = iter.next())){
				remove_internal(t);
				result = true;
			}
		}
		if(result){
			validateFinalState(new DummyTime());
		}
		return result;
	}
	
	public final boolean retainAll(SolutionEvent time, Collection<?> c){
		boolean result = false;
		
		Iterator<T> iter = super.iterator();
		for(T t; iter.hasNext();){
			if(!c.contains(t = iter.next())){
				remove_internal(time, t);
				result = true;
			}
		}
		if(result){
			validateFinalState(time);
		}
		return result;
	}
	
	public final void clear(SolutionEvent time){
		SafeRemovingIterator iter = new SafeRemovingIterator();
		while(iter.hasNext()){
			iter.next();
			iter.remove(time);
		}
	}
	
	@Override
	public final void clear(){
		Iterator<T> iter = iterator();
		while(iter.hasNext()){
			iter.next();
			iter.remove();
		}
	}
	
	@Override
	public final Iterator<T> iterator(){
		return new SafeRemovingIterator();
	}
	
	/**
	 * <p>An Iterator whose {@link Iterator#remove() remove()} method 
	 * calls the {@link Collection#remove() remove(Object)} method 
	 * of the removed element so as to remove this NodeSet from the 
	 * element that was removed from this NodeSet, guaranteeing 
	 * symmetry of connections in the graph.</p>
	 * @author fiveham
	 *
	 */
	private class SafeRemovingIterator implements Iterator<T>{
		private Iterator<T> wrappee = NodeSet.super.iterator();
		private T lastResult=null;
		@Override
		public void remove(){
			wrappee.remove();
			lastResult.remove(NodeSet.this);
		}
		public void remove(SolutionEvent time){
			wrappee.remove();
			lastResult.remove(time, NodeSet.this);
		}
		@Override
		public T next(){
			return lastResult = wrappee.next();
		}
		@Override
		public boolean hasNext(){
			return wrappee.hasNext();
		}
	}
	
	/**
	 * <p>Checks if this NodeSet obeys its own rules after some modification 
	 * operation has been performed on it, and enforces the consequences of 
	 * consequence-bearing states, including throwing an exception under 
	 * certain circumstances.</p>
	 * 
	 * <p>An empty method is provided here so that methods defined in this 
	 * class the need to validate the set's final state afterward can call 
	 * this method while subclasses provide meaningful implementations.</p>
	 */
	protected void validateFinalState(SolutionEvent time){
		//do nothing
	}
	
	@Override
	public Collection<T> neighbors(){
		return this;
	}
	
	public String toString(boolean bool){
		return toString();
	}
	
	private class DummyTime extends SolutionEvent{
		private DummyTime(){
		}
		public void pop(){
			//do nothing
		}
		public Time top(){
			return this;
		}
		public void push(Time time){
			//do nothing
		}
		public Set<Claim> falsified(){
			return new HashSet<Claim>(0);
		}
		public List<Time> children(){
			return new ArrayList<Time>(0);
		}
		public boolean addChild(Time child){
			return false;
		}
		@Override
		public boolean defers() {
			return false;
		}
		@Override
		public Time focus() {
			return this;
		}
		@Override
		public boolean hasChildren() {
			return false;
		}
		@Override
		public Time parent() {
			return null;
		}
		@Override
		public boolean hasParent() {
			return false;
		}
		@Override
		public Time currentTime() {
			return this;
		}
		@Override
		public List<Time> currentTrail() {
			return Collections.singletonList(this);
		}
		@Override
		public Time nextTime() {
			return this;
		}
		@Override
		public boolean hasNextTime() {
			return false;
		}
		@Override
		public boolean hasPrevTime() {
			return false;
		}
		@Override
		public Time prevTime() {
			return this;
		}
		@Override
		public Time nextChild() {
			return this;
		}
		@Override
		public boolean hasNextChild() {
			return false;
		}
		@Override
		public boolean hasPrevChild() {
			return false;
		}
		@Override
		public Time prevChild() {
			return this;
		}
		@Override
		public Time successor() {
			return this;
		}
		@Override
		public boolean hasSuccessor() {
			return false;
		}
		@Override
		public boolean hasPredecessor() {
			return false;
		}
		@Override
		public Time predecessor() {
			return this;
		}
		@Override
		public void toStart() {
			//do nothing
		}
		@Override
		public void toEnd() {
			//do nothing
		}
		@Override
		public Iterator<Time> iterator() {
			return currentTrail().iterator();
		}
	}
}