package project.model.identifiers;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

public abstract class Identifier<T> {

    protected final T value;

    public Identifier(final T value) {
        this.value = value;
    }

    public T getValue() {
        return this.value;
    }

    @Override
    public String toString() {
        return this.value.toString();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(this.value)
                .build();
    }

    @Override
    public boolean equals(final Object other) {
        if (!(other instanceof Identifier))
            return false;
        if (other == this)
            return true;

        final Identifier rhs = (Identifier) other;
        return new EqualsBuilder()
                        .append(this.value, rhs.getValue())
                        .isEquals();
    }
}