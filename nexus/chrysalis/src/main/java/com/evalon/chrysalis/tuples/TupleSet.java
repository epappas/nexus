package com.evalon.chrysalis.tuples;

/** User: Evangelos */
public interface TupleSet<X> {
	X get();

	<Y> TupleSet<Y> add(Y y);

	TupleSet left();
}
