/*
 * Copyright 2017-2019 original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.micronaut.configuration.metrics.micrometer.statsd

import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.composite.CompositeMeterRegistry
import io.micrometer.statsd.StatsdFlavor
import io.micrometer.statsd.StatsdMeterRegistry
import io.micronaut.configuration.metrics.micrometer.MeterRegistryCreationListener
import io.micronaut.context.ApplicationContext
import io.micronaut.context.env.Environment
import spock.lang.Specification
import spock.lang.Unroll

import java.time.Duration

import static io.micronaut.configuration.metrics.micrometer.MeterRegistryFactory.MICRONAUT_METRICS_ENABLED
import static io.micronaut.configuration.metrics.micrometer.statsd.StatsdMeterRegistryFactory.STATSD_CONFIG
import static io.micronaut.configuration.metrics.micrometer.statsd.StatsdMeterRegistryFactory.STATSD_ENABLED

class StatsdMeterRegistryFactorySpec extends Specification {

    def "wireup the bean manually"() {
        given:
        Environment mockEnvironment = Stub()
        mockEnvironment.getProperty(_, _) >> Optional.empty()

        when:
        StatsdMeterRegistryFactory factory = new StatsdMeterRegistryFactory(new StatsdConfigurationProperties(mockEnvironment))

        then:
        factory.statsdMeterRegistry()
    }

    void "verify StatsdMeterRegistry is created by default when this configuration used"() {
        when:
        ApplicationContext context = ApplicationContext.run()

        then:
        context.getBeansOfType(MeterRegistry).size() == 2
        context.getBeansOfType(MeterRegistry)*.class*.simpleName.containsAll(['CompositeMeterRegistry', 'StatsdMeterRegistry'])

        cleanup:
        context.stop()
    }

    void "verify CompositeMeterRegistry created by default"() {
        when:
        ApplicationContext context = ApplicationContext.run()
        CompositeMeterRegistry compositeRegistry = context.getBean(CompositeMeterRegistry)

        then:
        compositeRegistry
        context.getBean(MeterRegistryCreationListener)
        context.getBean(StatsdMeterRegistry)
        compositeRegistry.registries.size() == 1
        compositeRegistry.registries*.class.containsAll([StatsdMeterRegistry])

        cleanup:
        context.stop()
    }

    @Unroll
    void "verify StatsdMeterRegistry bean exists = #result when config #cfg = #setting"() {
        when:
        ApplicationContext context = ApplicationContext.run([(cfg): setting])

        then:
        context.findBean(StatsdMeterRegistry).isPresent() == result

        where:
        cfg                       | setting | result
        MICRONAUT_METRICS_ENABLED | false   | false
        MICRONAUT_METRICS_ENABLED | true    | true
        STATSD_ENABLED            | true    | true
        STATSD_ENABLED            | false   | false
    }

    void "verify StatsdMeterRegistry bean exists datadog flavor"() {
        when:
        ApplicationContext context = ApplicationContext.run([(STATSD_ENABLED): true, (STATSD_CONFIG + ".flavor"): StatsdFlavor.DATADOG])
        Optional<StatsdMeterRegistry> statsdMeterRegistry = context.findBean(StatsdMeterRegistry)

        then:
        statsdMeterRegistry.isPresent()
        statsdMeterRegistry.get().statsdConfig.enabled()
        statsdMeterRegistry.get().statsdConfig.flavor() == StatsdFlavor.DATADOG
        statsdMeterRegistry.get().statsdConfig.port() == 8125
        statsdMeterRegistry.get().statsdConfig.host() == "localhost"
        statsdMeterRegistry.get().statsdConfig.step() == Duration.ofMinutes(1)

        cleanup:
        context.stop()
    }

    void "verify StatsdMeterRegistry bean exists etsy flavor"() {
        when:
        ApplicationContext context = ApplicationContext.run([(STATSD_ENABLED): true, (STATSD_CONFIG + ".flavor"): StatsdFlavor.ETSY])
        Optional<StatsdMeterRegistry> statsdMeterRegistry = context.findBean(StatsdMeterRegistry)

        then:
        statsdMeterRegistry.isPresent()
        statsdMeterRegistry.get().statsdConfig.enabled()
        statsdMeterRegistry.get().statsdConfig.flavor() == StatsdFlavor.ETSY
        statsdMeterRegistry.get().statsdConfig.port() == 8125
        statsdMeterRegistry.get().statsdConfig.host() == "localhost"
        statsdMeterRegistry.get().statsdConfig.step() == Duration.ofMinutes(1)

        cleanup:
        context.stop()
    }

    void "verify StatsdMeterRegistry bean exists telegraf flavor"() {
        when:
        ApplicationContext context = ApplicationContext.run([(STATSD_ENABLED): true, (STATSD_CONFIG + ".flavor"): StatsdFlavor.TELEGRAF])
        Optional<StatsdMeterRegistry> statsdMeterRegistry = context.findBean(StatsdMeterRegistry)

        then:
        statsdMeterRegistry.isPresent()
        statsdMeterRegistry.get().statsdConfig.enabled()
        statsdMeterRegistry.get().statsdConfig.flavor() == StatsdFlavor.TELEGRAF
        statsdMeterRegistry.get().statsdConfig.port() == 8125
        statsdMeterRegistry.get().statsdConfig.host() == "localhost"
        statsdMeterRegistry.get().statsdConfig.step() == Duration.ofMinutes(1)

        cleanup:
        context.stop()
    }

    void "verify StatsdMeterRegistry bean exists datadog flavor changed port, host and step"() {
        when:
        ApplicationContext context = ApplicationContext.run([
                (STATSD_ENABLED)           : true,
                (STATSD_CONFIG + ".flavor"): StatsdFlavor.DATADOG,
                (STATSD_CONFIG + ".host")  : "127.0.0.1",
                (STATSD_CONFIG + ".port")  : 8122,
                (STATSD_CONFIG + ".step")  : "PT2M",
        ])
        Optional<StatsdMeterRegistry> statsdMeterRegistry = context.findBean(StatsdMeterRegistry)

        then:
        statsdMeterRegistry.isPresent()
        statsdMeterRegistry.get().statsdConfig.enabled()
        statsdMeterRegistry.get().statsdConfig.flavor() == StatsdFlavor.DATADOG
        statsdMeterRegistry.get().statsdConfig.port() == 8122
        statsdMeterRegistry.get().statsdConfig.host() == "127.0.0.1"
        statsdMeterRegistry.get().statsdConfig.step() == Duration.ofMinutes(2)

        cleanup:
        context.stop()
    }
}
