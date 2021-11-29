package de.foellix.aql.jicer.soot;

import soot.Local;
import soot.SootField;
import soot.Value;
import soot.jimple.ArrayRef;
import soot.jimple.FieldRef;
import soot.jimple.Ref;

public class ValueOrField {
	private boolean localOrRef;
	private boolean array;
	private Object object;

	public ValueOrField(Object object) {
		this.localOrRef = (object instanceof Local || object instanceof Ref);
		this.array = object instanceof ArrayRef;
		if (this.array) {
			this.object = ((ArrayRef) object).getBase();
		} else if (object instanceof FieldRef) {
			this.object = ((FieldRef) object).getField();
		} else if (object instanceof Value) {
			this.object = object;
		} else {
			throw new UnsupportedOperationException("Cannot handle " + object.getClass().getName() + " objects.");
		}
	}

	public boolean isArray() {
		return this.array;
	}

	public boolean isLocalOrRef() {
		return this.localOrRef;
	}

	public boolean isField() {
		return this.object instanceof SootField;
	}

	public SootField getField() {
		return (SootField) this.object;
	}

	public Value getValue() {
		return (Value) this.object;
	}

	public Object getValueOrField() {
		return this.object;
	}

	@Override
	public String toString() {
		if (isField()) {
			return getField().toString();
		} else {
			return getValue().toString();
		}
	}

	@Override
	public int hashCode() {
		if (isField()) {
			return getField().hashCode();
		} else {
			return getValue().hashCode();
		}
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		final ValueOrField other = (ValueOrField) obj;
		if (isField()) {
			if (other.isField()) {
				return getField() == other.getField();
			} else {
				return false;
			}
		} else {
			if (other.isField()) {
				return false;
			} else {
				return getValue() == other.getValue();
			}
		}
	}
}