# Copyright 2019 Cloudera
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#    http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

import base64
import logging
import six
import subprocess
import tempfile

from ipalib import Command, Flag
from ipalib import errors, output
from ipalib.parameters import Principal
from ipalib.plugable import Registry
from ipalib.text import _
from ipaserver.plugins.service import normalize_principal, validate_realm

if six.PY3:
    unicode = str

__doc__ = _("""
Retrieves a Kerberos keytab.

WARNING:  Retrieving the keytab resets the secret for the Kerberos principal.
This renders all other keytabs for that principal invalid.  When multiple hosts
or services need to share the same key (for instance in high availability or
load balancing  clusters),  the  retrieve  option  must  be  used  to
retrieve the existing key instead of generating a new one (please refer to the
EXAMPLES section).

NOTE: The retrieve option cannot be set before an initial keytab is created for
the Kerberos principal.


EXAMPLES:

 Create and retreive a keytab
   ipa get_keytab foo/example.com@EXAMPLE.COM

 Retrieve an existing keytab
   ipa get_keytab foo/example.com@EXAMPLE.COM --retrieve

""")

logger = logging.getLogger(__name__)

register = Registry()


@register()
class GetKeytab(Command):
    __doc__ = _('Retrieve a keytab.')

    name = 'get_keytab'

    takes_args = (
        Principal(
            'krbcanonicalname',
            validate_realm,
            cli_name='canonical_principal',
            label=_('Principal name'),
            doc=_('Kerberos principal'),
            primary_key=True,
            normalizer=normalize_principal
        )
    )

    takes_options = (
        Flag(
            'retrieve',
            doc=_('Retrieve an existing keytab (service principals only)'),
        )
    )

    has_output = (
        output.summary,
        output.Output(
            'result',
            dict,
            _('The keytab response which has a base64 encoded keytab element.')
        ),
    )

    # See https://web.mit.edu/kerberos/krb5-devel/doc/formats/keytab_file_format.html
    # for the keytab file format.
    #
    # NOTE: http://web.mit.edu/kerberos/www/krb5-1.12/doc/formats/keytab_file_format.html
    # has an incorrect size for the principal's count of components and it is
    # missing the entry's second key version.

    # Keytab header:
    #   [0] = 5 (KRB5)
    #   [1] = 2 (Big endian)
    __keytab_big_endian_header = b'\x05\x02'

    # The keytab record lenght is a signed 32 bit integers where 0 indicates
    # the end of records.
    __keytab_end_of_records = b'\x00\x00\x00\x00'

    # A keytab has a header followed by record lengths followed by records or
    # holes.
    __empty_keytab = __keytab_big_endian_header + __keytab_end_of_records

    def execute(self, krbcanonicalname, **options):
        retrieve = options.get('retrieve')

        principal_string = unicode(krbcanonicalname)

        keytab = self.get_keytab(principal_string,  retrieve)

        base64_keytab = base64.b64encode(keytab).decode('ascii')

        action = ('Created', 'Retrieved')[retrieve]
        summary = u'{0} keytab for principal "{1}"'.format(
            action, principal_string)

        return dict(
            summary=summary,
            result=dict(keytab=base64_keytab)
        )

    def get_keytab(self, principal, retrieve):
        action = ('creating', 'retrieving')[retrieve]
        logger.debug(u'%s keytab for principal "%s"', action, principal)

        try:
            # NOTE: This is run from httpd with PrivateTmp=yes so the
            # temporary files are not accessible to other services running on
            # the same VM. This is in addition to the security of the
            # discretionary access controls being locked down (using
            # NamedTemporaryFile creates files that are only accessible by the
            # user).
            with tempfile.NamedTemporaryFile(suffix='.keytab') as temp_keytab:

                # Since this uses a temporary file and ipa-getkkeytab tool can
                # either create a new keytab file or add to an existing keytab
                # file. A valid keytab file needs to be constructed.
                temp_keytab.write(self.__empty_keytab)
                temp_keytab.flush()

                args = ['ipa-getkeytab',
                        '-p', principal,
                        '-k', temp_keytab.name]
                if retrieve:
                    args.append('-r')

                # The ipa-getkeytab utility enforces the authorization policy
                # for retrieving the keytab.
                p = subprocess.Popen(args,
                                     stdout=subprocess.PIPE,
                                     stderr=subprocess.PIPE,
                                     close_fds=True,
                                     shell=False)
                # There is no stdout output from ipa-getkeytab
                (_, err) = p.communicate()
                p_status = p.wait()

                if p_status != 0:
                    logger.error(
                        u'ipa-getkeytab failed %s keytab for principal "%s" with error code %d and standard error '
                        u'message "%s"',
                        action, principal, p_status, err)
                    raise errors.InternalError()

                temp_keytab.seek(0)
                keytab = temp_keytab.read()

        except errors.InternalError:
            # The details were already logged
            raise
        except Exception as e:
            logger.error(u'failed %s keytab for principal "%s" with error "%s"', action, principal, e)
            raise

        logger.debug(u'%s keytab for principal "%s" succeeded', action, principal)

        return keytab
