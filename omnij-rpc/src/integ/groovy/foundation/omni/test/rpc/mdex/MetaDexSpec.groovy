package foundation.omni.test.rpc.mdex

import foundation.omni.BaseRegTestSpec
import foundation.omni.CurrencyID
import foundation.omni.Ecosystem
import foundation.omni.PropertyType
import org.junit.internal.AssumptionViolatedException
import spock.lang.Unroll

/**
 * Specification for the distributed token exchange
 */
class MetaDexSpec extends BaseRegTestSpec {

    final static BigDecimal startBTC = 0.001
    final static BigDecimal zeroAmount = 0.0
    final static Byte actionNew = 1

    /**
     * @see: https://github.com/OmniLayer/omnicore/issues/39
     */
    def "Orders are executed, if the effective price is within the accepted range of both"() {
        def actorA = createFundedAddress(startBTC, 0.00000006, false)
        def actorB = createFundedAddress(startBTC, zeroAmount, false)
        def actorC = createFundedAddress(startBTC, 0.00000001, false)
        def actorD = createFundedAddress(startBTC, 0.00000001, false)
        def propertyMSC = CurrencyID.MSC
        def propertySPX = fundNewProperty(actorB, 10, PropertyType.INDIVISIBLE, propertyMSC.ecosystem)

        when:
        def txidTradeA = trade_MP(actorA, propertyMSC, 0.00000006 , propertySPX, 6, actionNew)
        generateBlock()
        def txidTradeB = trade_MP(actorB, propertySPX, 10, propertyMSC, 0.00000001, actionNew)
        generateBlock()
        def txidTradeC = trade_MP(actorC, propertyMSC, 0.00000001, propertySPX, 10, actionNew)
        generateBlock()
        def txidTradeD = trade_MP(actorD, propertyMSC, 0.00000001, propertySPX, 4, actionNew)
        generateBlock()

        then:
        gettrade_MP(txidTradeA).valid
        gettrade_MP(txidTradeB).valid
        gettrade_MP(txidTradeC).valid
        gettrade_MP(txidTradeD).valid

        and:
        gettrade_MP(txidTradeA).status == "filled"
        gettrade_MP(txidTradeB).status == "filled"
        gettrade_MP(txidTradeC).status == "open"
        gettrade_MP(txidTradeD).status == "filled"

        and:
        getorderbook_MP(propertyMSC, propertySPX).size() == 1
        getorderbook_MP(propertySPX, propertyMSC).size() == 0

        and:
        getbalance_MP(actorC, propertyMSC).reserved == 0.00000001
    }

    /**
     * @see: https://github.com/OmniLayer/omnicore/issues/37
     */
    def "Two orders can match, and both can be partially filled"() {
        def actorA = createFundedAddress(startBTC, zeroAmount, false)
        def actorB = createFundedAddress(startBTC, 0.55, false)
        def propertyMSC = CurrencyID.MSC
        def propertySPX = fundNewProperty(actorA, 25, PropertyType.INDIVISIBLE, propertyMSC.ecosystem)

        when:
        def txidTradeA = trade_MP(actorA, propertySPX, 25, propertyMSC, 2.50, actionNew)
        generateBlock()
        def txidTradeB = trade_MP(actorB, propertyMSC, 0.55, propertySPX, 5, actionNew)
        generateBlock()

        then:
        gettrade_MP(txidTradeA).valid
        gettrade_MP(txidTradeB).valid

        and:
        gettrade_MP(txidTradeA).status != "open"
        gettrade_MP(txidTradeB).status != "open"
        gettrade_MP(txidTradeA).status != "filled"
        gettrade_MP(txidTradeB).status != "filled"

        and:
        getbalance_MP(actorA, propertyMSC).balance == 0.5
        getbalance_MP(actorA, propertySPX).reserved == 20 as BigDecimal
        getbalance_MP(actorB, propertySPX).balance == 5 as BigDecimal
        getbalance_MP(actorB, propertyMSC).reserved == 0.05
    }

    /**
     * @see: https://github.com/OmniLayer/omnicore/issues/38
     */
    def "Orders fill with the maximal amount at the best price possible"() {
        def actorA = createFundedAddress(startBTC, 0.00000020, false)
        def actorB = createFundedAddress(startBTC, zeroAmount, false)
        def propertyMSC = CurrencyID.TMSC
        def propertySPX = fundNewProperty(actorB, 12, PropertyType.INDIVISIBLE, propertyMSC.ecosystem)

        when:
        def txidTradeA = trade_MP(actorA, propertyMSC, 0.00000020 , propertySPX, 10, actionNew)
        generateBlock()
        def txidTradeB = trade_MP(actorB, propertySPX, 12, propertyMSC, 0.00000017, actionNew)
        generateBlock()

        then:
        gettrade_MP(txidTradeA).valid
        gettrade_MP(txidTradeB).valid

        and:
        gettrade_MP(txidTradeA).status == "filled"
        gettrade_MP(txidTradeB).status != "open"
        gettrade_MP(txidTradeB).status != "filled"

        and:
        getbalance_MP(actorA, propertySPX).balance == 10 as BigDecimal
        getbalance_MP(actorA, propertyMSC).reserved == 0.0
        getbalance_MP(actorB, propertyMSC).balance == 0.00000020
        getbalance_MP(actorB, propertySPX).reserved == 2 as BigDecimal
    }

    /**
     * @see: https://github.com/OmniLayer/omnicore/issues/41
     */
    def "Orders with inverted price and different amounts for sale match"() {
        def actorA = createFundedAddress(startBTC, zeroAmount, false)
        def actorB = createFundedAddress(startBTC, 1.66666666, false)
        def propertyMSC = CurrencyID.MSC
        def propertySPX = fundNewProperty(actorA, 1.0, PropertyType.DIVISIBLE, propertyMSC.ecosystem)

        when:
        def txidTradeA = trade_MP(actorA, propertySPX, 1.0, propertyMSC, 2.0, actionNew)
        generateBlock()
        def txidTradeB = trade_MP(actorB, propertyMSC, 1.66666666, propertySPX, 0.83333333, actionNew)
        generateBlock()

        then:
        gettrade_MP(txidTradeA).valid
        gettrade_MP(txidTradeB).valid

        and:
        gettrade_MP(txidTradeA).status != "open"
        gettrade_MP(txidTradeA).status != "filled"
        gettrade_MP(txidTradeB).status == "filled"

        and:
        getbalance_MP(actorA, propertyMSC).balance == 1.66666666
        getbalance_MP(actorA, propertySPX).reserved == 0.16666667
        getbalance_MP(actorB, propertySPX).balance == 0.83333333
        getbalance_MP(actorB, propertyMSC).reserved == 0.0
    }

    /**
     * @see: https://github.com/OmniLayer/omnicore/issues/40
     */
    def "Intermediate results are not truncated or rounded"() {
        def actorA = createFundedAddress(startBTC, 23.0, false)
        def actorB = createFundedAddress(startBTC, zeroAmount, false)
        def propertyMSC = CurrencyID.TMSC
        def propertySPX = fundNewProperty(actorB, 50.0, PropertyType.DIVISIBLE, propertyMSC.ecosystem)

        when:
        def txidTradeA = trade_MP(actorA, propertyMSC, 23.0 , propertySPX, 100.0, actionNew)
        generateBlock()
        def txidTradeB = trade_MP(actorB, propertySPX, 50.0, propertyMSC, 10.0, actionNew)
        generateBlock()

        then:
        gettrade_MP(txidTradeA).valid
        gettrade_MP(txidTradeB).valid

        and:
        gettrade_MP(txidTradeA).status != "open"
        gettrade_MP(txidTradeA).status != "filled"
        gettrade_MP(txidTradeB).status == "filled"

        and:
        getbalance_MP(actorA, propertySPX).balance == 50.0
        getbalance_MP(actorA, propertyMSC).reserved == 11.5
        getbalance_MP(actorB, propertyMSC).balance == 11.5
        getbalance_MP(actorB, propertySPX).reserved == 0.0
    }

    def "One side of the trade must either be MSC or TMSC"() {
        def actorAdress = createFundedAddress(startBTC, zeroAmount, false)
        def propertySPA = fundNewProperty(actorAdress, 20.0, PropertyType.DIVISIBLE, Ecosystem.TMSC)
        def propertySPB = fundNewProperty(actorAdress, 10.0, PropertyType.DIVISIBLE, Ecosystem.TMSC)

        when:
        def txidTrade = trade_MP(actorAdress, propertySPA, 1.0, propertySPB, 1.0, actionNew)
        generateBlock()

        then:
        gettrade_MP(txidTrade).valid == false

        and:
        getbalance_MP(actorAdress, propertySPA).balance == 20.0
        getbalance_MP(actorAdress, propertySPB).balance == 10.0
        getbalance_MP(actorAdress, propertySPA).reserved == zeroAmount
        getbalance_MP(actorAdress, propertySPB).reserved == zeroAmount
    }

    @Unroll
    def "Exact trade match: #amountMSC MSC for #amountSPX SPX"() {
        when:
        def traderA = createFundedAddress(startBTC, amountMSC, false)  // offers MSC
        def traderB = createFundedAddress(startBTC, zeroAmount, false) // offers SPX
        def propertySPX = fundNewProperty(traderB, amountSPX, propertyTypeSPX, propertyMSC.ecosystem)

        then:
        getbalance_MP(traderA, propertyMSC).balance == amountMSC
        getbalance_MP(traderA, propertySPX).balance == zeroAmount
        getbalance_MP(traderB, propertyMSC).balance == zeroAmount
        getbalance_MP(traderB, propertySPX).balance == amountSPX

        when: "trader A offers MSC and desired SPX"
        def txidOfferA = trade_MP(traderA, propertyMSC, amountMSC, propertySPX, amountSPX, actionNew)
        generateBlock()

        then: "it is a valid open order"
        gettrade_MP(txidOfferA).valid
        gettrade_MP(txidOfferA).status == "open"
        gettrade_MP(txidOfferA).propertyidforsale == propertyMSC.longValue()
        gettrade_MP(txidOfferA).propertyiddesired == propertySPX.longValue()

        and: "there is an offering for the new property in the orderbook"
        getorderbook_MP(propertyMSC, propertySPX).size() == 1

        and: "the offered amount is now reserved"
        getbalance_MP(traderA, propertyMSC).balance == zeroAmount
        getbalance_MP(traderA, propertyMSC).reserved == amountMSC

        when: "trader B offers SPX and desires MSC"
        def txidOfferB = trade_MP(traderB, propertySPX, amountSPX, propertyMSC, amountMSC, actionNew)
        generateBlock()

        then: "the order is filled"
        gettrade_MP(txidOfferB).valid
        gettrade_MP(txidOfferB).status == "filled"
        gettrade_MP(txidOfferB).propertyidforsale == propertySPX.longValue()
        gettrade_MP(txidOfferB).propertyiddesired == propertyMSC.longValue()

        and: "the offering is no longer listed in the orderbook"
        getorderbook_MP(propertyMSC, propertySPX).size() == 0

        and:
        getbalance_MP(traderA, propertyMSC).balance == zeroAmount
        getbalance_MP(traderA, propertySPX).balance == amountSPX
        getbalance_MP(traderB, propertyMSC).balance == amountMSC
        getbalance_MP(traderB, propertySPX).balance == zeroAmount

        and:
        getbalance_MP(traderA, propertyMSC).reserved == zeroAmount
        getbalance_MP(traderA, propertySPX).reserved == zeroAmount
        getbalance_MP(traderB, propertyMSC).reserved == zeroAmount
        getbalance_MP(traderB, propertySPX).reserved == zeroAmount

        where:
        propertyTypeSPX          | amountSPX                              | propertyMSC     | amountMSC
        PropertyType.DIVISIBLE   | new BigDecimal("3.0")                  | CurrencyID.MSC  | new BigDecimal("6.0")
        PropertyType.DIVISIBLE   | new BigDecimal("2.0")                  | CurrencyID.TMSC | new BigDecimal("0.6")
        PropertyType.DIVISIBLE   | new BigDecimal("50.0")                 | CurrencyID.MSC  | new BigDecimal("0.1")
        PropertyType.DIVISIBLE   | new BigDecimal("0.99999999")           | CurrencyID.TMSC | new BigDecimal("0.11111111")
        PropertyType.DIVISIBLE   | new BigDecimal("3333.33333333")        | CurrencyID.MSC  | new BigDecimal("0.0125")
        PropertyType.DIVISIBLE   | new BigDecimal("0.03")                 | CurrencyID.TMSC | new BigDecimal("0.009")
        PropertyType.DIVISIBLE   | new BigDecimal("0.00000001")           | CurrencyID.MSC  | new BigDecimal("0.00000003")
        PropertyType.DIVISIBLE   | new BigDecimal("10000000001.0")        | CurrencyID.TMSC | new BigDecimal("0.00000002")
        PropertyType.DIVISIBLE   | new BigDecimal("92233720368.54775807") | CurrencyID.MSC  | new BigDecimal("0.00000001")
        PropertyType.INDIVISIBLE | new BigDecimal("7")                    | CurrencyID.TMSC | new BigDecimal("0.33333333")
        PropertyType.INDIVISIBLE | new BigDecimal("1")                    | CurrencyID.MSC  | new BigDecimal("0.00000001")
        PropertyType.INDIVISIBLE | new BigDecimal("33333")                | CurrencyID.TMSC | new BigDecimal("0.0001")
        PropertyType.INDIVISIBLE | new BigDecimal("4815162342")           | CurrencyID.MSC  | new BigDecimal("0.00101011")
        PropertyType.INDIVISIBLE | new BigDecimal("1000000000000000051")  | CurrencyID.TMSC | new BigDecimal("0.00000001")
        PropertyType.INDIVISIBLE | new BigDecimal("9223372036854775807")  | CurrencyID.MSC  | new BigDecimal("0.00000006")
    }

    @Unroll
    def "Exact trade match: #amountSPX SPX for #amountMSC MSC"() {
        when:
        def traderA = createFundedAddress(startBTC, zeroAmount, false) // offers SPX
        def traderB = createFundedAddress(startBTC, amountMSC, false)  // offers MSC
        def propertySPX = fundNewProperty(traderA, amountSPX, propertyTypeSPX, propertyMSC.ecosystem)

        then:
        getbalance_MP(traderA, propertyMSC).balance == zeroAmount
        getbalance_MP(traderA, propertySPX).balance == amountSPX
        getbalance_MP(traderB, propertyMSC).balance == amountMSC
        getbalance_MP(traderB, propertySPX).balance == zeroAmount

        when: "trader A offers SPX and desired MSC"
        def txidOfferA = trade_MP(traderA, propertySPX, amountSPX, propertyMSC, amountMSC, actionNew)
        generateBlock()

        then: "it is a valid open order"
        gettrade_MP(txidOfferA).valid
        gettrade_MP(txidOfferA).status == "open"
        gettrade_MP(txidOfferA).propertyidforsale == propertySPX.longValue()
        gettrade_MP(txidOfferA).propertyiddesired == propertyMSC.longValue()

        and: "there is an offering for the new property in the orderbook"
        getorderbook_MP(propertySPX, propertyMSC).size() == 1

        and: "the offered amount is now reserved"
        getbalance_MP(traderA, propertySPX).balance == zeroAmount
        getbalance_MP(traderA, propertySPX).reserved == amountSPX

        when: "trader B offers MSC and desires SPX"
        def txidOfferB = trade_MP(traderB, propertyMSC, amountMSC, propertySPX, amountSPX, actionNew)
        generateBlock()

        then: "the order is filled"
        gettrade_MP(txidOfferB).valid
        gettrade_MP(txidOfferB).status == "filled"
        gettrade_MP(txidOfferB).propertyidforsale == propertyMSC.longValue()
        gettrade_MP(txidOfferB).propertyiddesired == propertySPX.longValue()

        and: "the offering is no longer listed in the orderbook"
        getorderbook_MP(propertySPX, propertyMSC).size() == 0

        and:
        getbalance_MP(traderA, propertyMSC).balance == amountMSC
        getbalance_MP(traderA, propertySPX).balance == zeroAmount
        getbalance_MP(traderB, propertyMSC).balance == zeroAmount
        getbalance_MP(traderB, propertySPX).balance == amountSPX

        and:
        getbalance_MP(traderA, propertyMSC).reserved == zeroAmount
        getbalance_MP(traderA, propertySPX).reserved == zeroAmount
        getbalance_MP(traderB, propertyMSC).reserved == zeroAmount
        getbalance_MP(traderB, propertySPX).reserved == zeroAmount

        where:
        propertyTypeSPX          | amountSPX                              | propertyMSC     | amountMSC
        PropertyType.INDIVISIBLE | new BigDecimal("101")                  | CurrencyID.MSC  | new BigDecimal("0.1")
        PropertyType.DIVISIBLE   | new BigDecimal("100.02")               | CurrencyID.MSC  | new BigDecimal("0.01")
        PropertyType.INDIVISIBLE | new BigDecimal("1000003")              | CurrencyID.MSC  | new BigDecimal("0.001")
        PropertyType.DIVISIBLE   | new BigDecimal("10000.0004")           | CurrencyID.MSC  | new BigDecimal("0.0001")
        PropertyType.INDIVISIBLE | new BigDecimal("100000000005")         | CurrencyID.MSC  | new BigDecimal("0.00001")
        PropertyType.DIVISIBLE   | new BigDecimal("1000000.0000006")      | CurrencyID.MSC  | new BigDecimal("0.000001")
        PropertyType.INDIVISIBLE | new BigDecimal("1000000000000007")     | CurrencyID.MSC  | new BigDecimal("0.0000001")
        PropertyType.DIVISIBLE   | new BigDecimal("100000000.00000008")   | CurrencyID.TMSC | new BigDecimal("0.00000007")
        PropertyType.INDIVISIBLE | new BigDecimal("444444444444444449")   | CurrencyID.TMSC | new BigDecimal("0.00000051")
        PropertyType.DIVISIBLE   | new BigDecimal("92233720368.54775807") | CurrencyID.TMSC | new BigDecimal("0.00000001")
        PropertyType.INDIVISIBLE | new BigDecimal("353535")               | CurrencyID.TMSC | new BigDecimal("0.07171717")
        PropertyType.DIVISIBLE   | new BigDecimal("0.0000513")            | CurrencyID.TMSC | new BigDecimal("0.00005511")
        PropertyType.INDIVISIBLE | new BigDecimal("100")                  | CurrencyID.TMSC | new BigDecimal("0.00002222")
        PropertyType.DIVISIBLE   | new BigDecimal("0.77777776")           | CurrencyID.TMSC | new BigDecimal("0.00000333")
        PropertyType.INDIVISIBLE | new BigDecimal("9223372036854775807")  | CurrencyID.TMSC | new BigDecimal("0.00000021")
    }

    def setupSpec() {
        if (!commandExists("gettrade_MP")) {
            throw new AssumptionViolatedException('The client has no "gettrade_MP" command')
        }
        if (!commandExists("getorderbook_MP")) {
            throw new AssumptionViolatedException('The client has no "getorderbook_MP" command')
        }
    }

}
