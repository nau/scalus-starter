//> using scala 3.3.1
//> using plugin org.scalus:scalus-plugin_3:0.4.0
//> using dep org.scalus:scalus_3:0.4.0
package starter

import io.bullet.borer.Cbor
import scalus.Compile
import scalus.Compiler.compile
import scalus.*
import scalus.builtins.Builtins
import scalus.builtins.ByteString
import scalus.builtins.given
import scalus.ledger.api.PlutusLedgerLanguage
import scalus.ledger.api.v2.FromDataInstances.given
import scalus.ledger.api.v2.ScriptPurpose.*
import scalus.ledger.api.v2.*
import scalus.prelude.AssocMap
import scalus.prelude.List
import scalus.prelude.Maybe.*
import scalus.prelude.Prelude.===
import scalus.prelude.Prelude.given
import scalus.pretty
import scalus.sir.SIR
import scalus.sir.SimpleSirToUplcLowering
import scalus.uplc.Cek
import scalus.uplc.Data
import scalus.uplc.Data.fromData
import scalus.uplc.Program
import scalus.uplc.ProgramFlatCodec
import scalus.uplc.Term
import scalus.utils.Utils

/* This annotation is used to generate the Scalus Intermediate Representation (SIR)
   for the code in the annotated object.
 */
@Compile
object MintingPolicy {
    import List.*

    def mintingPolicyScript(
        txId: ByteString, // TxId and output index we must spend to mint tokens
        txOutIdx: BigInt,
        tokensToMint: AssocMap[ByteString, BigInt]
    ) = (redeemer: Unit, ctxData: Data) => {
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
                txOutRefTxId.hash === txId && txOutRefIdx === txOutIdx
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
    val compiledMintingPolicyScript = compile(MintingPolicy.mintingPolicyScript)
    val validator = compiledMintingPolicyScript.toUplc(generateErrorTraces = true)

    def makeMintingPolicy(txOutRefToSpend: TxOutRef, tokensToMint: SIR): Program =
        import scalus.uplc.TermDSL.{*, given}
        val evaledTokens =
            val tokens = tokensToMint.toUplc()
            Cek.evalUPLC(tokens) // simplify the term

        val appliedValidator =
            validator $ txOutRefToSpend.id.hash $ txOutRefToSpend.idx $ evaledTokens
        Program((1, 0, 0), appliedValidator)
}

/*
 This is an example of how to use the MintingPolicyGenerator to generate a minting policy validator.
 We simulate minting of HOSKY tokens. The tokens were minted by spending a 'hoskyMintTxOutRef'
 */
object HoskyMintingPolicyValidator {
    val hoskyMintTxOutRef = TxOutRef(
      TxId(ByteString.fromHex("1ab6879fc08345f51dc9571ac4f530bf8673e0d798758c470f9af6f98e2f3982")),
      0
    )
    val hoskyMintTxOut = TxOut(
      Address(
        Credential.PubKeyCredential(
          PubKeyHash(
            ByteString.fromHex("61822dde476439a526070f36d3d1667ad099b462c111cd85e089f5e7f6")
          )
        ),
        Nothing
      ),
      Value.lovelace(BigInt("10000000")),
      OutputDatum.NoOutputDatum,
      Nothing
    )

    /*
    We need to parameterize the minting policy validator with the AssocMap of tokens to mint.
    We 'compile' the AssocMap to a SIR term
     */
    val tokensToMint: SIR = compile(
      AssocMap.singleton(ByteString.fromHex("484f534b59"), BigInt("1000000000000000"))
    )

    // UPLC term of the minting policy validator
    val script = MintingPolicyGenerator.makeMintingPolicy(hoskyMintTxOutRef, tokensToMint)
    val cbor = script.cborEncoded

    @main def main() = {
        println("Hosky minting policy validator:")
        // Pretty print the minting policy validator's SIR
        println(MintingPolicyGenerator.compiledMintingPolicyScript.pretty.render(100))
        println("Hosky minting policy validator double CBOR:")
        // Double CBOR encoded UPLC term
        // That's what you need to construct a minting transaction
        println(script.doubleCborHex)
        // Write the minting policy validator to a .plutus file for use with cardano-cli etc
        Utils.writePlutusFile("minting.plutus", script, PlutusLedgerLanguage.PlutusV2)
    }
}
