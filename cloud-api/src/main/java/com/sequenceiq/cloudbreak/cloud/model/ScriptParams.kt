package com.sequenceiq.cloudbreak.cloud.model

/**

 * The 'diskPrefix' and 'startLabel' parameters are part of the disk naming pattern on different platforms

 * e.g. on AWS the disk naming pattern is /dev/xvda, /dev/xvdb, ...
 * where the 'diskPrefix' is 'xvd' and the 'startLabel' is the ascii code of 'a' which is '97'
 */
class ScriptParams(val diskPrefix: String, val startLabel: Int?)
