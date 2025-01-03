package starter

import com.bloxbean.cardano.client.plutus.spec.PlutusV3Script
import scalus.*
import scalus.Compiler.compile
import scalus.builtin.ByteString.StringInterpolators
import scalus.builtin.Data.FromData
import scalus.builtin.Data.ToData
import scalus.builtin.Data.fromData
import scalus.builtin.Data.toData
import scalus.builtin.FromData
import scalus.builtin.ToData
import scalus.builtin.{ByteString, Data}
import scalus.ledger.api.PlutusLedgerLanguage
import scalus.ledger.api.v2.OutputDatum
import scalus.ledger.api.v3.*
import scalus.builtin.ToDataInstances.given
import scalus.builtin.FromDataInstances.given
import scalus.ledger.api.v1.FromDataInstances.given
import scalus.ledger.api.v1.ToDataInstances.given
import scalus.ledger.api.v3.FromDataInstances.given
import scalus.ledger.api.v3.ScriptPurpose.*
import scalus.prelude.Maybe.*
import scalus.prelude.Prelude.given
import scalus.prelude.{AssocMap, List}
import scalus.sir.SIR
import scalus.uplc.Program
import scalus.utils.Utils
import starter.MintingPolicy.MintingConfig

import scala.language.implicitConversions

/* This annotation is used to generate the Scalus Intermediate Representation (SIR)
   for the code in the annotated object.
 */
@Compile
/** Minting policy script
  */
object MintingPolicy {

    case class MintingConfig(
        adminPubKeyHash: PubKeyHash,
        tokenName: TokenName
    )

    given FromData[MintingConfig] = FromData.deriveCaseClass[MintingConfig]
    given ToData[MintingConfig] = ToData.deriveCaseClass[MintingConfig](0)

    /** Minting policy script
      *
      * @param adminPubKeyHash
      *   admin public key hash
      * @param tokenName
      *   token name to mint or burn
      * @param ctx
      *   [[ScriptContext]]
      */
    def mintingPolicy(
        adminPubKeyHash: PubKeyHash, // admin pub key hash
        tokenName: TokenName, // token name
        ctx: ScriptContext
    ): Unit = {
        // ensure that we are minting and get the PolicyId of the token we are minting
        val ownSymbol = ctx.scriptInfo match
            case ScriptInfo.MintingScript(curSymbol) => curSymbol
            case _ => throw new IllegalArgumentException("Not a minting transaction")
        val txInfo = ctx.txInfo
        // find the tokens minted by this policy id
        AssocMap.lookup(txInfo.mint)(ownSymbol) match
            case Just(mintedTokens) =>
                AssocMap.toList(mintedTokens) match
                    // there should be only one token with the given name
                    case List.Cons((tokName, _), tail) =>
                        tail match
                            case List.Nil =>
                                if tokName == tokenName then ()
                                else throw new IllegalArgumentException("Token name not found")
                            case _ =>
                                throw new IllegalArgumentException("Multiple tokens found")
                    case _ =>
                        throw new IllegalArgumentException(
                          "Tokens not found or multiple tokens found"
                        )
            case Nothing =>
                // should not happen on-chain
                throw new IllegalArgumentException("Tokens not found")

        // only admin can mint or burn tokens
        List.findOrFail(txInfo.signatories): signatory =>
            signatory.hash == adminPubKeyHash.hash
    }

    /** Minting policy validator
      *
      * The validator is parameterized by the [[MintingConfig]] which is passed as [[Data]] before
      * the validator is published on-chain.
      *
      * @param config
      *   minting policy configuration
      * @param ctxData
      *   context data
      */
    def mintingPolicyValidator(config: Data)(ctxData: Data): Unit = {
        // deserialize the context from Data to ScriptContext
        val ctx = ctxData.to[ScriptContext]
        val mintingConfig = config.to[MintingConfig]
        mintingPolicy(mintingConfig.adminPubKeyHash, mintingConfig.tokenName, ctx)
    }
}

object MintingPolicyGenerator {
    val mintingPolicySIR: SIR = compile(MintingPolicy.mintingPolicyValidator)
    private val script = mintingPolicySIR.toUplcOptimized(generateErrorTraces = true).plutusV3

    def makeMintingPolicyScript(
        adminPubKeyHash: PubKeyHash,
        tokenName: TokenName
    ): MintingPolicyScript = {
        import scalus.uplc.TermDSL.{*, given}

        val config = MintingPolicy
            .MintingConfig(adminPubKeyHash = adminPubKeyHash, tokenName = tokenName)
        MintingPolicyScript(script = script $ config.toData)
    }
}

class MintingPolicyScript(val script: Program) {
    lazy val plutusScript: PlutusV3Script = PlutusV3Script
        .builder()
        .`type`("PlutusScriptV3")
        .cborHex(script.doubleCborHex)
        .build()
        .asInstanceOf[PlutusV3Script]

    lazy val scriptHash: ByteString = ByteString.fromArray(plutusScript.getScriptHash)
}
