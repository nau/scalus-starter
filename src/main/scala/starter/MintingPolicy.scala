package starter

import scalus.*
import scalus.Compiler.compile
import scalus.builtin.ByteString.StringInterpolators
import scalus.builtin.Data.fromData
import scalus.builtin.{ByteString, Data}
import scalus.ledger.api.PlutusLedgerLanguage
import scalus.ledger.api.v2.*
import scalus.ledger.api.v2.FromDataInstances.given
import scalus.ledger.api.v2.ScriptPurpose.*
import scalus.prelude.Maybe.*
import scalus.prelude.Prelude.given
import scalus.prelude.{AssocMap, List}
import scalus.sir.SIR
import scalus.uplc.Program
import scalus.uplc.eval.VM
import scalus.utils.Utils

/* This annotation is used to generate the Scalus Intermediate Representation (SIR)
   for the code in the annotated object.
 */
@Compile
object MintingPolicy {
    import List.*

    def mintingPolicyScript(
        txId: ByteString, // TxId and output index we must spend to mint tokens
        txOutIdx: BigInt, // TxOut index we must spend to mint tokens
        tokensToMint: AssocMap[ByteString, BigInt] // tokens to mint
    )(redeemer: Unit, ctxData: Data): Unit = {
        // deserialize the context from Data to ScriptContext
        val ctx = fromData[ScriptContext](ctxData)
        // ensure that we are minting and get the PolicyId of the token we are minting
        val ownSymbol = ctx.purpose match
            case Minting(curSymbol) => curSymbol
            case _                  => throw new Exception("M")
        val txInfo = ctx.txInfo
        val txOutRefs = List.map(txInfo.inputs)(_.outRef)
        // find the tokens minted by this policy id
        val mintedTokens = AssocMap.lookup(txInfo.mint)(ownSymbol) match
            case Just(mintedTokens) => mintedTokens
            case Nothing            => throw new Exception("T")

        val foundTxOutWeMustSpend = List.find(txOutRefs) {
            case TxOutRef(txOutRefTxId, txOutRefIdx) =>
                txOutRefTxId.hash == txId && txOutRefIdx == txOutIdx
        }

        val check = (b: Boolean, msg: String) => if b then () else throw new Exception(msg)

        foundTxOutWeMustSpend match
            // If the transaction spends the TxOut, then it's a minting transaction
            case Just(input) => check(Value.equalsAssets(mintedTokens, tokensToMint), "M")
            // Otherwise, it's a burn transaction
            case Nothing =>
                // check burned
                val burned = List.all(mintedTokens.inner) { case (tokenName, amount) =>
                    amount < 0
                }
                check(burned, "B")
    }
}

object MintingPolicyGenerator {
    val compiledMintingPolicySIR: SIR = compile(MintingPolicy.mintingPolicyScript)
    private val validator = compiledMintingPolicySIR.toUplc(generateErrorTraces = true)

    def makeMintingPolicyProgram(txOutRefToSpend: TxOutRef, tokensToMint: SIR): Program =
        import scalus.uplc.TermDSL.{*, given}
        val evaledTokens =
            val tokens = tokensToMint.toUplc()
            VM.evaluateTerm(tokens) // simplify the term

        val appliedValidator =
            validator $ txOutRefToSpend.id.hash $ txOutRefToSpend.idx $ evaledTokens
        Program((1, 0, 0), appliedValidator)
}

/*
 This is an example of how to use the MintingPolicyGenerator to generate a minting policy validator.
 We simulate minting of HOSKY tokens. The tokens were minted by spending a 'hoskyMintTxOutRef'
 */
object HoskyMintingPolicyValidator {
    val hoskyMintTxOutRef: TxOutRef =
        TxOutRef(TxId(hex"1ab6879fc08345f51dc9571ac4f530bf8673e0d798758c470f9af6f98e2f3982"), 0)
    val hoskyMintTxOut: TxOut = TxOut(
      Address(
        Credential.PubKeyCredential(
          PubKeyHash(hex"61822dde476439a526070f36d3d1667ad099b462c111cd85e089f5e7f6")
        ),
        Nothing
      ),
      Value.lovelace(10_000_000),
      OutputDatum.NoOutputDatum,
      Nothing
    )

    /*
    We need to parameterize the minting policy validator with the AssocMap of tokens to mint.
    We 'compile' the AssocMap to a SIR term
     */
    private val tokensToMint: SIR = compile(
      AssocMap.singleton(hex"484f534b59", BigInt("1000000000000000"))
    )

    // UPLC program of the minting policy validator
    val mintingPolicyProgram: Program =
        MintingPolicyGenerator.makeMintingPolicyProgram(hoskyMintTxOutRef, tokensToMint)

    @main def main(): Unit = {
        println("Hosky minting policy validator:")
        // Pretty print the minting policy validator's SIR
        println(MintingPolicyGenerator.compiledMintingPolicySIR.prettyXTerm.render(100))
        println("Hosky minting policy validator double CBOR:")
        // Double CBOR encoded UPLC term
        // That's what you need to construct a minting transaction
        println(mintingPolicyProgram.doubleCborHex)
        // Write the minting policy validator to a .plutus file for use with cardano-cli etc
        Utils.writePlutusFile("minting.plutus", mintingPolicyProgram, PlutusLedgerLanguage.PlutusV2)
    }
}
