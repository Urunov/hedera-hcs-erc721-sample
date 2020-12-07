package com.hedera.hashgraph.seven_twenty_one.contract;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import com.hedera.hashgraph.sdk.AccountId;
import com.hedera.hashgraph.sdk.PrivateKey;
import com.hedera.hashgraph.sdk.TopicId;
import com.hedera.hashgraph.sdk.TransactionId;
import com.hedera.hashgraph.seven_twenty_one.proto.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.Set;

public class TransferFromTest {
    State state = new State();
    TopicListener topicListener = new TopicListener(state, null, new TopicId(0), null);

    @Test
    public void transferFromTest() throws InvalidProtocolBufferException {
        var callerKey = PrivateKey.generate();
        var callerAddress = new Address(callerKey.getPublicKey());
        var callerAccount = AccountId.fromString("0.0.5005");
        var toKey = PrivateKey.generate();
        var toAddress = new Address(toKey.getPublicKey());
        var baseURI = "baseURI";
        var tokenId = new Int(BigInteger.valueOf(555));

        // prepare state
        var tokenName = "tokenName";
        var tokenSymbol = "tokenSymbol";

        // Build constructor function
        var constructorFunctionDataBuilder = ConstructorFunctionData.newBuilder();

        var constructorFunctionData = constructorFunctionDataBuilder
                .setTokenName(tokenName)
                .setTokenSymbol(tokenSymbol)
                .setBaseURI(baseURI)
                .build();

        var constructorTransactionId = TransactionId.generate(callerAccount);
        var constructorValidStartNanos = ChronoUnit.NANOS.between(
                Instant.EPOCH,
                constructorTransactionId.validStart
        );

        var constructorFunctionBody = FunctionBody
                .newBuilder()
                .setCaller(ByteString.copyFrom(callerKey.getPublicKey().toBytes()))
                .setOperatorAccountNum(callerAccount.num)
                .setValidStartNanos(constructorValidStartNanos)
                .setConstruct(constructorFunctionData)
                .build();

        var constructorFunctionBodyBytes = constructorFunctionBody.toByteArray();
        var constructorFunctionSignature = callerKey.sign(constructorFunctionBodyBytes);

        var constructorFunction = Function
                .newBuilder()
                .setBody(ByteString.copyFrom(constructorFunctionBodyBytes))
                .setSignature(ByteString.copyFrom(constructorFunctionSignature))
                .build();

        // Build mint function (minting to owner)

        var mintFunctionDataBuilder = MintFunctionData.newBuilder();

        var mintFunctionData = mintFunctionDataBuilder
                .setId(ByteString.copyFrom(tokenId.value.toByteArray()))
                .setTo(callerAddress.toByteString())
                .build();

        var mintTransactionId = TransactionId.generate(callerAccount);
        var mintValidStartNanos = ChronoUnit.NANOS.between(
                Instant.EPOCH,
                mintTransactionId.validStart
        );

        var mintFunctionBody = FunctionBody
                .newBuilder()
                .setCaller(ByteString.copyFrom(callerKey.getPublicKey().toBytes()))
                .setOperatorAccountNum(callerAccount.num)
                .setValidStartNanos(mintValidStartNanos)
                .setMint(mintFunctionData)
                .build();

        var mintFunctionBodyBytes = mintFunctionBody.toByteArray();
        var mintFunctionSignature = callerKey.sign(mintFunctionBodyBytes);

        var mintFunction = Function
                .newBuilder()
                .setBody(ByteString.copyFrom(mintFunctionBodyBytes))
                .setSignature(ByteString.copyFrom(mintFunctionSignature))
                .build();

        // Build transferFrom function (from owner to toAddress)

        var transferFromFunctionDataBuilder = TransferFromFunctionData.newBuilder();

        var transferFromFunctionData = transferFromFunctionDataBuilder
                .setId(ByteString.copyFrom(tokenId.value.toByteArray()))
                .setFrom(callerAddress.toByteString())
                .setTo(toAddress.toByteString())
                .build();

        var transferFromTransactionId = TransactionId.generate(callerAccount);
        var transferFromValidStartNanos = ChronoUnit.NANOS.between(
                Instant.EPOCH,
                transferFromTransactionId.validStart
        );

        var transferFromFunctionBody = FunctionBody
                .newBuilder()
                .setCaller(ByteString.copyFrom(callerKey.getPublicKey().toBytes()))
                .setOperatorAccountNum(callerAccount.num)
                .setValidStartNanos(transferFromValidStartNanos)
                .setTransferFrom(transferFromFunctionData)
                .build();

        var transferFromFunctionBodyBytes = transferFromFunctionBody.toByteArray();
        var transferFromFunctionSignature = callerKey.sign(transferFromFunctionBodyBytes);

        var transferFromFunction = Function
                .newBuilder()
                .setBody(ByteString.copyFrom(transferFromFunctionBodyBytes))
                .setSignature(ByteString.copyFrom(transferFromFunctionSignature))
                .build();

        // Construct and mint before Pre-Check
        topicListener.handleFunction(constructorFunction);
        topicListener.handleFunction(mintFunction);

        // Pre-Check

        // i. Owner != 0x
        Assertions.assertNotNull(state.getOwner());

        // ii. TokenOwners[id] != 0x
        Assertions.assertNotNull(state.getTokenOwner(tokenId));

        // iii. TokenOwners[id] = caller || TokenApprovals[id] = caller ||
        // OperatorApprovals[TokenOwners[id]][caller]
        Assertions.assertEquals(callerAddress, state.getTokenOwner(tokenId));

        // iv. TokenOwners[id] = from
        Assertions.assertEquals(callerAddress, state.getTokenOwner(tokenId));

        // v. to != 0x
        Assertions.assertNotNull(toAddress);

        // Prepare for post-check
        var preHolderTokensFrom = state.getTokens(toAddress);
        Set<Int> postHolderTokensFrom = new HashSet<>(preHolderTokensFrom);
        postHolderTokensFrom.remove(tokenId);

        var preHolderTokensTo = state.getTokens(toAddress);
        Set<Int> postHolderTokensTo = new HashSet<>(preHolderTokensTo);
        postHolderTokensTo.add(tokenId);

        // Update State
        topicListener.handleFunction(transferFromFunction);


        // Post-Check

        // i. HolderTokens[from]’ = HolderTokens[from] \ {id}
        Assertions.assertEquals(postHolderTokensFrom, state.getTokens(callerAddress));

        // ii. HolderTokens[to]’ = HolderTokens[to] + {id}
        Assertions.assertEquals(postHolderTokensTo, state.getTokens(toAddress));

        // iii. TokenOwners[id] = to
        Assertions.assertEquals(toAddress, state.getTokenOwner(tokenId));

        // iv. TokenApprovals[id] = 0x
        Assertions.assertNull(state.getTokenApprovals(tokenId));
    }
}
