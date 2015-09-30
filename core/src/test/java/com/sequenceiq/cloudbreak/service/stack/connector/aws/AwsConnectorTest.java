package com.sequenceiq.cloudbreak.service.stack.connector.aws;

public class AwsConnectorTest {
//    private static final String DUMMY_NUMBER_STR = "1";
//    private static final int DUMMY_NUMBER = 1;
//    private static final String DUMMY_SERVICE = "Dummy service";
//
//    @Mock
//    private AwsStackUtil awsStackUtil;
//
//    @Mock
//    private EventBus reactor;
//
//    @Mock
//    private AmazonCloudFormationClient amazonCloudFormationClient;
//
//    @Mock
//    private AmazonEC2Client ec2Client;
//
//    @Mock
//    private CloudFormationStackUtil cloudFormationStackUtil;
//
//    @Mock
//    private AmazonAutoScalingClient amazonAutoScalingClient;
//
//    @Mock
//    private DescribeAutoScalingGroupsResult describeAutoScalingGroupsResult;
//
//    @Mock
//    private PollingService<AwsInstances> awsPollingService;
//
//    @Mock
//    private PollingService<AutoScalingGroupReadyContext> autoScalingGroupReadyPollingService;
//
//    @Mock
//    private PollingService<CloudFormationStackContext> cloudFormationPollingService;
//
//    @Mock
//    private PollingService<EbsVolumeContext> ebsVolumeStatePollingService;
//
//    @Mock
//    private PollingService<SnapshotReadyContext> snapshotReadyPollingService;
//
//    @Mock
//    private PollingService<ConsoleOutputContext> consoleOutputPollingService;
//
//    @Mock
//    private CloudFormationStackStatusChecker cloudFormationStackStatusChecker;
//
//    @InjectMocks
//    private AwsConnector underTest;
//
//    // Domain models
//
//    private Stack stack;
//
//    private Credential credential;
//
//    private Template awsTemplate;
//
//    private DescribeStacksResult stackResult;
//
//    private DescribeInstancesResult instancesResult;
//
//    private DescribeStackResourcesResult stackResourcesResult;
//
//    @Before
//    public void setUp() {
//        underTest = new AwsConnector();
//        MockitoAnnotations.initMocks(this);
//        awsTemplate = ServiceTestUtils.createTemplate(CloudPlatform.AWS);
//        credential = ServiceTestUtils.createCredential(CloudPlatform.AWS);
//        stack = ServiceTestUtils.createStack(awsTemplate, credential, getDefaultResourceSet());
//        instancesResult = ServiceTestUtils.createDescribeInstanceResult();
//    }
//
//    public Set<Resource> getDefaultResourceSet() {
//        Set<Resource> resources = new HashSet<>();
//        resources.add(new Resource(ResourceType.CLOUDFORMATION_STACK, ConnectorTestUtil.CF_STACK_NAME, stack, "master"));
//        return resources;
//    }
//
//    @Test
//    public void testDeleteStack() {
//        // GIVEN
//        instancesResult.setReservations(generateReservationsWithInstances());
//        given(awsStackUtil.createEC2Client(Regions.DEFAULT_REGION, (AwsCredential) credential)).willReturn(ec2Client);
//        given(ec2Client.describeInstances(any(DescribeInstancesRequest.class))).willReturn(instancesResult);
//        given(awsStackUtil.createCloudFormationClient(any(Regions.class), any(AwsCredential.class))).willReturn(amazonCloudFormationClient);
//        given(amazonCloudFormationClient.describeStacks(any(DescribeStacksRequest.class))).willReturn(new DescribeStacksResult());
//        given(cloudFormationStackUtil.getAutoscalingGroupName(any(Stack.class), anyString())).willReturn("test");
//        given(cloudFormationStackUtil.getAutoscalingGroupName(any(Stack.class), any(AmazonCloudFormationClient.class), anyString())).willReturn("test");
//        given(awsStackUtil.createAutoScalingClient(any(Regions.class), any(AwsCredential.class))).willReturn(amazonAutoScalingClient);
//        given(amazonAutoScalingClient.describeAutoScalingGroups(any(DescribeAutoScalingGroupsRequest.class))).willReturn(describeAutoScalingGroupsResult);
//        given(describeAutoScalingGroupsResult.getAutoScalingGroups()).willReturn(new ArrayList<AutoScalingGroup>());
//        given(cloudFormationPollingService.pollWithTimeout(any(StatusCheckerTask.class), any(CloudFormationStackContext.class), anyInt(), anyInt()))
//                .willReturn(PollingResult.SUCCESS);
//        doNothing().when(amazonCloudFormationClient).deleteStack(any(DeleteStackRequest.class));
//        // WHEN
//        underTest.deleteStack(stack, credential);
//        // THEN
//        verify(amazonCloudFormationClient, times(1)).deleteStack(any(DeleteStackRequest.class));
//    }
//
//    @Test
//    public void testDeleteStackWithoutInstanceResultReservations() {
//        // GIVEN
//        instancesResult.setReservations(new ArrayList<Reservation>());
//        given(awsStackUtil.createEC2Client(Regions.DEFAULT_REGION, (AwsCredential) credential)).willReturn(ec2Client);
//        given(ec2Client.describeInstances(any(DescribeInstancesRequest.class))).willReturn(instancesResult);
//        given(awsStackUtil.createCloudFormationClient(any(Regions.class), any(AwsCredential.class))).willReturn(amazonCloudFormationClient);
//        given(amazonCloudFormationClient.describeStacks(any(DescribeStacksRequest.class))).willReturn(new DescribeStacksResult());
//        given(cloudFormationStackUtil.getAutoscalingGroupName(any(Stack.class), anyString())).willReturn("test");
//        given(cloudFormationStackUtil.getAutoscalingGroupName(any(Stack.class), any(AmazonCloudFormationClient.class), anyString())).willReturn("test");
//        given(awsStackUtil.createAutoScalingClient(any(Regions.class), any(AwsCredential.class))).willReturn(amazonAutoScalingClient);
//        given(amazonAutoScalingClient.describeAutoScalingGroups(any(DescribeAutoScalingGroupsRequest.class))).willReturn(describeAutoScalingGroupsResult);
//        given(describeAutoScalingGroupsResult.getAutoScalingGroups()).willReturn(new ArrayList<AutoScalingGroup>());
//        given(cloudFormationPollingService.pollWithTimeout(any(StatusCheckerTask.class), any(CloudFormationStackContext.class), anyInt(), anyInt()))
//                .willReturn(PollingResult.SUCCESS);
//        doNothing().when(amazonCloudFormationClient).deleteStack(any(DeleteStackRequest.class));
//        // WHEN
//        underTest.deleteStack(stack, credential);
//        // THEN
//        verify(amazonCloudFormationClient, times(1)).deleteStack(any(DeleteStackRequest.class));
//    }
//
//    private AmazonServiceException createAmazonServiceException() {
//        AmazonServiceException e = new AmazonServiceException(String.format("Stack:%s does not exist", ConnectorTestUtil.CF_STACK_NAME));
//        e.setServiceName("AmazonCloudFormation");
//        e.setErrorCode(DUMMY_NUMBER_STR);
//        e.setRequestId(DUMMY_NUMBER_STR);
//        e.setStatusCode(DUMMY_NUMBER);
//        return e;
//    }
//
//    private List<Reservation> generateReservationsWithInstances() {
//        List<Reservation> reservations = Lists.newArrayList();
//        for (int i = 0; i < 5; i++) {
//            Reservation r = new Reservation();
//            List<Instance> instances = Lists.newArrayList();
//            instances.add(new Instance().withInstanceId(String.valueOf(new Random().nextInt(100))));
//            r.setInstances(instances);
//            reservations.add(r);
//        }
//        return reservations;
//    }

}
