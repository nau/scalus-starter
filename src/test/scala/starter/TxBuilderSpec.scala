package starter

import com.bloxbean.cardano.client.common.model.Networks
import scalus.*
import scalus.builtin.ByteString
import scalus.ledger.api.v3.*

class TxBuilderSpec extends munit.ScalaCheckSuite {

    private val tokenName = "CO2 Tonne"
    private lazy val appCtx =
        AppCtx(
          Networks.preprod(),
          System.getenv("MNEMONIC"),
          System.getenv("BLOCKFROST_API_KEY"),
          tokenName
        )

    private val pubKey: ByteString = ByteString.fromArray(appCtx.account.publicKeyBytes())

    test("create minting transaction") {
        val txBuilder = TxBuilder(appCtx)
        txBuilder.makeMintingTx(1000) match
            case Right(tx) =>
                assertEquals(
                  ByteString.fromArray(tx.getBody.getMint.get(0).getAssets.get(0).getNameAsBytes),
                  appCtx.tokenNameByteString
                )
                assertEquals(tx.getWitnessSet.getVkeyWitnesses.size(), 1)
                assertEquals(
                  ByteString.fromArray(tx.getWitnessSet.getVkeyWitnesses.get(0).getVkey),
                  pubKey
                )
            case Left(err) => fail(err)
    }

}
