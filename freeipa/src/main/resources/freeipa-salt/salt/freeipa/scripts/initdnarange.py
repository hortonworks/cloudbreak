from ipalib import api
from ipaserver.install import replication
import logging
from random import randint
from time import sleep


class RangeSet:
    def __init__(self, elements):
        self.ranges = list(elements)

    def __iter__(self):
        return iter(self.ranges)

    def __repr__(self):
        return 'RangeSet: %r' % self.ranges

    def has(self, tup):
        for pos, i in enumerate(self.ranges):
            if i[0] <= tup[0] and i[1] >= tup[1]:
                return pos, i
        raise ValueError('Invalid range or overlapping range')

    def minus(self, tup):
        pos, (x, y) = self.has(tup)
        out = []
        if x < tup[0]:
            out.append((x, tup[0] - 1))
        if y > tup[1]:
            out.append((tup[1] + 1, y))
        self.ranges[pos:pos + 1] = out

    def __sub__(self, r):
        r1 = RangeSet(self)
        for i in r: r1.minus(i)
        return r1

    def sub(self, r):  # inplace subtraction
        for i in r:
            self.minus(i)

    def subWithResult(self, result, r):  # unfortunately ipa console won't work with the __sub__ solution, couldn't initialize RangeSet
        result.ranges = self.ranges
        for i in r:
            result.minus(i)
        return result


class DnaRangeIniter:
    """Needed a class as FreeIPA console couldn't resolved method to method calls"""

    MIN_RANGE_SIZE = 1000

    def __init__(self, repl):
        """Console couldn't resolve the import otherwise in class methods"""
        self.replication = repl
        self.localReplManager = self.replication.ReplicationManager(api.env.realm, api.env.host, starttls=True, port=389)

    def validate_dna_range_missing(self):
        dna_range_start, dna_range_end = self.localReplManager.get_DNA_range(api.env.host)
        print ("DNA range on this server: {} - {}".format(dna_range_start, dna_range_end))
        if dna_range_start is not None:
            print ("DNA range is already set on this server, exiting")
            exit(0)

        dna_next_range_start, dna_next_range_end = self.localReplManager.get_DNA_next_range(api.env.host)
        print ("DNA next range on this server: {} - {}".format(dna_next_range_start, dna_next_range_end))
        if dna_next_range_start is not None:
            print ("DNA next range is already set on this server, exiting")
            exit(0)

    def get_all_servers_fqdn(self):
        servers_resp = api.Command.server_find(None, pkey_only=True)['result']
        servers = []
        for server in servers_resp:
            servers.append(server['cn'][0])
        print("Found servers: {}".format(servers))
        return servers

    def get_ranges_from_server(self, fqdn):
        repl_manager = self.replication.ReplicationManager(api.env.realm, fqdn, starttls=True, port=389)
        dna_range_start, dna_range_end = repl_manager.get_DNA_range(fqdn)
        print ("DNA range on {} server: {} - {}".format(fqdn, dna_range_start, dna_range_end))
        dna_next_range_start, dna_next_range_end = repl_manager.get_DNA_next_range(fqdn)
        print ("DNA next range on {} server: {} - {}".format(fqdn, dna_next_range_start, dna_next_range_end))
        result = []
        if dna_range_start is not None:
            result.append((dna_range_start, dna_range_end))
        if dna_next_range_start is not None:
            result.append((dna_next_range_start, dna_next_range_end))
        return result

    def grab_all_assigned_ranges(self):
        servers = self.get_all_servers_fqdn()
        ranges = []
        for server in servers:
            ranges_from_server = self.get_ranges_from_server(server)
            ranges.extend(ranges_from_server)
        print ("All ranges from servers: {}".format(ranges))
        return ranges

    def grab_original_range(self):
        idrange_result = api.Command.idrange_show(api.env.realm + "_id_range")['result']
        print ("Id range response: ", idrange_result)
        ipabaseid = int(idrange_result['ipabaseid'][0])
        ipaidrangesize = int(idrange_result['ipaidrangesize'][0])
        print ("ipabaseid: {} ipaidrangesize: {}".format(ipabaseid, ipaidrangesize))
        return ipabaseid, ipabaseid + ipaidrangesize

    def setRanges(self, freeRanges):
        if len(freeRanges) == 0:
            print ("There  no free ranges to set")
        else:
            print ("Available free ranges: ", freeRanges)
            for freeRange in freeRanges:
                if freeRange[1] - freeRange[0] < self.MIN_RANGE_SIZE:
                    print ("Range {} is smaller than the minimal size [{}]".format(freeRange, self.MIN_RANGE_SIZE))
                else:
                    try:
                        print ("Try to set next range to ", freeRange)
                        ret = self.localReplManager.save_DNA_next_range(freeRange[0], freeRange[1])
                        if ret:
                            print ("Successfully set next range")
                            return
                        else:
                            print ("Next range was not set as it has been set already")
                            return
                    except Exception:
                        logging.exception("Couldn't set the range. Most probably another instance got it first. Trying another one if there is. Ex: ")


def create_and_delete_user():
    user_for_test = api.env.host.split('.')[0] + "rangeinit"
    print ("Add user [{}] to ensure dna range is assigned".format(user_for_test))
    api.Command.user_add(user_for_test, givenname=u'idrange', sn=u'allocation')
    print ("Delete user [{}]".format(user_for_test))
    api.Command.user_del(user_for_test)
    print ("User [{}] deleted".format(user_for_test))


rangeIniter = DnaRangeIniter(replication)

rangeIniter.validate_dna_range_missing()
originalRangeSet = RangeSet([(rangeIniter.grab_original_range())])
try:
    assignedRanges = RangeSet(rangeIniter.grab_all_assigned_ranges())
    freeRanges = originalRangeSet.subWithResult(RangeSet([]), assignedRanges).ranges
except ValueError:
    logging.exception("Calculating failed, most probably the ranges returned are out of sync. Retry... Ex: ")
    sleep(randint(3,10))
    assignedRanges = RangeSet(rangeIniter.grab_all_assigned_ranges())
    freeRanges = originalRangeSet.subWithResult(RangeSet([]), assignedRanges).ranges

print ("Free ranges: ", freeRanges)
rangeIniter.setRanges(freeRanges)
create_and_delete_user()
