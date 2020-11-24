package com.hedera.hashgraph.seven_twenty_one.contract;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import javax.annotation.Nullable;

public final class State {

    // used to lock write access to state during state snapshot serialization
    private final Lock lock = new ReentrantLock();

    // timestamp that this state was last updated
    // null = never updated
    @Nullable
    private Instant timestamp;

    // mapping from holder addresses to their tokens
    private final Map<Address, Set<Int>> holderTokens;

    // mapping from token ID to the token owner address
    private final Map<Int, Address> tokenOwners;

    // mapping from token ID to approved addresses
    private final Map<Int, Address> tokenApprovals;

    private final Map<Address, Map<Address, Boolean>> operatorApprovals;

    private final Map<Int, String> tokenURIs;

    // each Hedera contract has an owner
    @Nullable
    private Address owner;

    @Nullable
    private String tokenName;

    @Nullable
    private String tokenSymbol;

    @Nullable
    private String baseURI;

    public State() {
        holderTokens = new HashMap<>();
        tokenOwners = new HashMap<>();
        tokenApprovals = new HashMap<>();
        operatorApprovals = new HashMap<>();
        tokenURIs = new HashMap<>();
    }

    public void lock() {
        lock.lock();
    }

    public void unlock() {
        lock.unlock();
    }

    @Nullable
    public Instant getTimestamp() {
        return timestamp;
    }

    @Nullable
    public Address getOwner() {
        return owner;
    }

    @Nullable
    public Address getTokenOwner(Int tokenId) {
        return tokenOwners.get(tokenId);
    }

    public boolean isApproved(Address caller, Int tokenId) {
        var tokenApproval = tokenApprovals.get(tokenId);

        return tokenApproval != null && tokenApproval.equals(caller);
    }

    public boolean isOperatorApproved(Address caller, Int tokenId) {
        var operatorApproval = operatorApprovals.get(
                Objects.requireNonNull(tokenOwners.get(tokenId)));

        return operatorApproval != null && operatorApproval.getOrDefault(caller, false);
    }

    public void setTokenApproval(Int tokenId, Address spender) {
        tokenApprovals.put(tokenId, spender);
    }
}
