package scalus

import org.scalatest.funsuite.AnyFunSuite
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import scalus.Compiler.compile
import scalus.builtins.ByteString
import scalus.builtins.ByteString.given
import scalus.ledger.api.v2.*
import scalus.prelude.*
import scalus.sir.SimpleSirToUplcLowering
import scalus.uplc.TermDSL.{_, given}
import scalus.uplc.*
import starter.HoskyMintingPolicyValidator

enum Expected {
  case Success(value: Term)
  case Failure(reason: String)
}

class MintingPolicySpec extends AnyFunSuite with ScalaCheckPropertyChecks {
  import Expected.*

  val hoskyMintTxOutRef = HoskyMintingPolicyValidator.hoskyMintTxOutRef
  val hoskyMintTxOut = HoskyMintingPolicyValidator.hoskyMintTxOut

  test("validator size is correct") {
    val size = HoskyMintingPolicyValidator.script.cborEncoded.length
    assert(size == 2429)
  }

  test("should succeed when the TxOutRef is spent and the minted tokens are correct") {
    // The minting policy script should succeed when the TxOutRef is spent and the minted tokens are correct
    assertEval(
      withScriptContextV2(
        List(TxInInfo(hoskyMintTxOutRef, hoskyMintTxOut)),
        Value(
          hex"a0028f350aaabe0545fdcb56b039bfb08e4bb4d8c4d7c3c7d481c235",
          hex"484f534b59",
          BigInt("1000000000000000")
        )
      ),
      Success(())
    )
  }

  test("should successfully burn, the minted tokens are negative and TxOutRef is not spent") {
    assertEval(
      withScriptContextV2(
        List.empty,
        Value(
          hex"a0028f350aaabe0545fdcb56b039bfb08e4bb4d8c4d7c3c7d481c235",
          hex"484f534b59",
          BigInt(-100)
        )
      ),
      Success(())
    )
  }

  test("should fail when the TxOutRef is not spent") {
    assertEval(
      withScriptContextV2(
        List.empty,
        Value(
          hex"a0028f350aaabe0545fdcb56b039bfb08e4bb4d8c4d7c3c7d481c235",
          hex"484f534b59",
          BigInt("1000000000000000")
        )
      ),
      Failure("TxOutRef not spent")
    )
  }

  test("should fail when the minted tokens are negative") {
    assertEval(
      withScriptContextV2(
        List(TxInInfo(hoskyMintTxOutRef, hoskyMintTxOut)),
        Value(
          hex"a0028f350aaabe0545fdcb56b039bfb08e4bb4d8c4d7c3c7d481c235",
          hex"484f534b59",
          BigInt(-100)
        )
      ),
      Failure("Wrong amount")
    )
  }

  test("should fail when given the wrong Policy ID") {
    assertEval(
      withScriptContextV2(
        List(TxInInfo(hoskyMintTxOutRef, hoskyMintTxOut)),
        Value(hex"cc", hex"484f534b59", BigInt("1000000000000000"))
      ),
      Failure("Wrong Policy ID")
    )
  }

  private def scriptContextV2(txInfoInputs: scalus.prelude.List[TxInInfo], value: Value) =
    ScriptContext(
      TxInfo(
        inputs = txInfoInputs,
        referenceInputs = scalus.prelude.List.Nil,
        outputs = scalus.prelude.List.Nil,
        fee = Value.lovelace(BigInt("188021")),
        mint = value,
        dcert = scalus.prelude.List.Nil,
        withdrawals = AssocMap.empty,
        validRange = Interval.always,
        signatories = scalus.prelude.List.Nil,
        redeemers = AssocMap.empty,
        data = AssocMap.empty,
        id = TxId(hex"1e0612fbd127baddfcd555706de96b46c4d4363ac78c73ab4dee6e6a7bf61fe9")
      ),
      ScriptPurpose.Minting(hex"a0028f350aaabe0545fdcb56b039bfb08e4bb4d8c4d7c3c7d481c235")
    )

  def withScriptContextV2(txInfoInputs: scalus.prelude.List[TxInInfo], value: Value) =
    import Data.toData
    import scalus.ledger.api.v2.ToDataInstances.given
    HoskyMintingPolicyValidator.script.copy(term =
      HoskyMintingPolicyValidator.script.term $ () $ scriptContextV2(txInfoInputs, value).toData
    )

  def assertEval(p: Program, expected: Expected) = {
    val result = PlutusUplcEval.evalFlat(p)
    (result, expected) match
      case (UplcEvalResult.Success(result), Expected.Success(expected)) =>
        assert(result == expected)
      case (UplcEvalResult.UplcFailure(code, error), Expected.Failure(expected)) =>
      case _ => fail(s"Unexpected result: $result, expected: $expected")
  }
}
