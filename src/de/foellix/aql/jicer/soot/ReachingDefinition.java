package de.foellix.aql.jicer.soot;

import java.util.HashSet;
import java.util.Set;

import soot.Unit;

public class ReachingDefinition {
	private ValueOrField valueOrField;
	private Set<Unit> units;
	private Set<ValueOrField> aliases;

	public ReachingDefinition(ValueOrField valueOrField) {
		super();
		this.valueOrField = valueOrField;
		this.units = new HashSet<>();
		this.aliases = new HashSet<>();
	}

	@Override
	public String toString() {
		return "(" + this.valueOrField + ", " + this.units + ", " + this.aliases + ")";
	}

	public ValueOrField getValueOrField() {
		return this.valueOrField;
	}

	public Set<Unit> getUnits() {
		return this.units;
	}

	public Set<ValueOrField> getAliases() {
		return this.aliases;
	}

	public boolean hasAliases() {
		return !this.aliases.isEmpty();
	}

	@Override
	public int hashCode() {
		return this.valueOrField.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		final ReachingDefinition other = (ReachingDefinition) obj;
		if (this.valueOrField != other.valueOrField) {
			return false;
		}
		if (this.units.size() != other.getUnits().size() || !this.units.containsAll(other.getUnits())) {
			return false;
		}
		if (this.aliases.size() != other.getAliases().size() || !this.aliases.containsAll(other.getAliases())) {
			return false;
		}
		return true;
	}

	public ReachingDefinition copy() {
		final ReachingDefinition copy = new ReachingDefinition(this.valueOrField);
		copy.getUnits().addAll(this.units);
		copy.getAliases().addAll(this.aliases);
		return copy;
	}
}