package foundation.omni.test.rpc.basic

import foundation.omni.BaseRegTestSpec

import static foundation.omni.CurrencyID.*

class ConsensusSpec extends BaseRegTestSpec {

    def "Check all balances"() {
        when: "we check Mastercoin balances"
        def balances = getallbalancesforid_MP(MSC)

        then: "we get a list of size >= 0"
        balances != null
        balances.size() >= 0
    }

    def "Check all balances, raw CLI, type Long"() {
        when: "we check Mastercoin balances"
        def balances = cliSend("getallbalancesforid_MP", MSC as Long)

        then: "we get a list of size >= 0"
        balances != null
        balances.size() >= 0
    }

    def "Throw exception checking all balances, raw CLI, type String"() {
        when: "we check Mastercoin balances"
        cliSend("getallbalancesforid_MP", "1")

        then: "Exception is thrown"
        Exception e = thrown()
    }

}
