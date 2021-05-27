/*
 * Copyright 2020-2021 University of Oxford and Health and Social Care Information Centre, also known as NHS Digital
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
import ch.qos.logback.classic.filter.ThresholdFilter
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.filter.Filter
import ch.qos.logback.core.spi.FilterReply
import org.springframework.boot.logging.logback.ColorConverter
import org.springframework.boot.logging.logback.WhitespaceThrowableProxyConverter

import java.nio.charset.Charset

conversionRule 'clr', ColorConverter
conversionRule 'wex', WhitespaceThrowableProxyConverter


// Message
String nonAnsiPattern = '%d{ISO8601} [%10.10thread] %-5level %-40.40logger{39} : %msg%n'

def buildDir = new File('.', 'build').canonicalFile
def logDir = new File(buildDir, 'logs').canonicalFile
if (!logDir) logDir.mkdirs()

String logFileName = buildDir.parentFile.name

String logMsg = "==> Log File available at ${logDir}/${logFileName}.log <=="

println(logMsg)

// See http://logback.qos.ch/manual/groovy.html for details on configuration
appender('STDOUT', ConsoleAppender) {
    encoder(PatternLayoutEncoder) {
        charset = Charset.forName('UTF-8')
        pattern = nonAnsiPattern
    }

    filter(ThresholdFilter) {
        level = INFO
    }
    filter(HibernateMappingFilter)
    filter HibernateDeprecationFilter
    filter HibernateNarrowingFilter
}

appender("FILE", FileAppender) {
    file = "${logDir}/${logFileName}.log"
    append = true

    encoder(PatternLayoutEncoder) {
        pattern = nonAnsiPattern
    }
    filter(HibernateMappingFilter)
    filter HibernateDeprecationFilter
    filter HibernateNarrowingFilter

    filter(ThresholdFilter) {
        level = TRACE
    }
}
root(INFO, ['STDOUT', 'FILE'])

logger('uk.ac.ox.softeng', DEBUG)

logger('org.springframework.jdbc.core.JdbcTemplate', DEBUG)

logger('org.apache.lucene', DEBUG)
logger('org.hibernate.search.fulltext_query', DEBUG)
logger('org.hibernate.search.batchindexing.impl', WARN)
//    logger('org.hibernate.SQL', DEBUG)
// logger 'org.hibernate.type', TRACE
logger('org.flyway', DEBUG)
// Track interceptor order

logger('org.grails.spring.beans.factory.OptimizedAutowireCapableBeanFactory', ERROR)
logger('org.springframework.context.support.PostProcessorRegistrationDelegate', WARN)
logger('org.hibernate.cache.ehcache.AbstractEhcacheRegionFactory', ERROR)
logger 'org.hibernate.tool.schema.internal.ExceptionHandlerLoggedImpl', ERROR
logger 'org.hibernate.engine.jdbc.spi.SqlExceptionHelper', ERROR

logger 'org.springframework.mock.web.MockServletContext', ERROR
logger 'StackTrace', OFF

logger 'uk.ac.ox.softeng.maurodatamapper.datamodel', TRACE

class HibernateMappingFilter extends Filter<ILoggingEvent> {

    @Override
    FilterReply decide(ILoggingEvent event) {
        event.message ==~ /.*Specified config option \[importFrom\].*/ ? FilterReply.DENY : FilterReply.NEUTRAL
    }
}

class HibernateDeprecationFilter extends Filter<ILoggingEvent> {

    @Override
    FilterReply decide(ILoggingEvent event) {
        event.message ==~ /HHH90000022.*/ ? FilterReply.DENY : FilterReply.NEUTRAL
    }
}

class HibernateNarrowingFilter extends Filter<ILoggingEvent> {

    @Override
    FilterReply decide(ILoggingEvent event) {
        event.message ==~ /HHH000179.*/ ? FilterReply.DENY : FilterReply.NEUTRAL
    }
}
