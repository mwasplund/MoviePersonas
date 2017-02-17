package project.model.netflix;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import project.model.identifiers.UserId;

public class UserPair {

    private final UserId activeUser;
    private final UserId otherUser;

    public UserPair(final UserId activeUser,
                    final UserId otherUser) {
        this.activeUser = activeUser;
        this.otherUser = otherUser;
    }

    public UserId getActiveUserId() {
        return this.activeUser;
    }

    public UserId getOtherUserId() {
        return this.otherUser;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(this.activeUser)
                .append(this.otherUser)
                .build();
    }

    @Override
    public boolean equals(final Object other) {
        if (!(other instanceof UserRating))
            return false;
        if (other == this)
            return true;

        final UserPair rhs = (UserPair) other;
        return new EqualsBuilder()
                        .append(this.activeUser, rhs.getActiveUserId())
                        .append(this.otherUser, rhs.getOtherUserId())
                        .isEquals();
    }

    @Override
    public String toString() {
        return String.format("{active user: %d, other user %d}",
                        this.activeUser.getValue(),
                        this.otherUser.getValue());
    }
}