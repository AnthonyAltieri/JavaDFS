package conformance2;

import test.*;

/** Runs all conformance tests on distributed filesystem components.

    <p>
    Tests performed are:
    <ul>
    <li>{@link conformance2.common.PathTest}</li>
    <li>{@link conformance2.rmi.SkeletonTest}</li>
    <li>{@link conformance2.rmi.StubTest}</li>
    <li>{@link conformance2.rmi.ConnectionTest}</li>
    <li>{@link conformance2.rmi.ThreadTest}</li>
    <li>{@link conformance2.storage.RegistrationTest}</li>
    <li>{@link conformance2.storage.AccessTest}</li>
    <li>{@link conformance2.storage.DirectoryTest}</li>
    <li>{@link conformance2.storage.ReplicationTest}</li>
    <li>{@link conformance2.naming.ContactTest}</li>
    <li>{@link conformance2.naming.RegistrationTest}</li>
    <li>{@link conformance2.naming.ListingTest}</li>
    <li>{@link conformance2.naming.CreationTest}</li>
    <li>{@link conformance2.naming.StubRetrievalTest}</li>
    <li>{@link conformance2.naming.LockTest}</li>
    <li>{@link conformance2.naming.QueueTest}</li>
    <li>{@link conformance2.naming.ReplicationTest}</li>
    <li>{@link conformance2.naming.DeletionTest}</li>
    </ul>
 */
public class ConformanceTests
{
    /** Runs the tests.

        @param arguments Ignored.
     */
    public static void main(String[] arguments)
    {
        // Create the test list, the series object, and run the test series.
        @SuppressWarnings("unchecked")
        Class<? extends Test>[]     tests =
            new Class[] {
                conformance2.common.PathTest.class,
                 conformance2.rmi.SkeletonTest.class,
                 conformance2.rmi.StubTest.class,
                 conformance2.rmi.ConnectionTest.class,
                 conformance2.rmi.ThreadTest.class,
                 conformance2.storage.RegistrationTest.class,
                 conformance2.storage.AccessTest.class,
                 conformance2.storage.DirectoryTest.class,
                 conformance2.storage.ReplicationTest.class,
                 conformance2.naming.ContactTest.class,
                 conformance2.naming.RegistrationTest.class,
                 conformance2.naming.ListingTest.class,
                 conformance2.naming.CreationTest.class,
                 conformance2.naming.StubRetrievalTest.class,
                 conformance2.naming.LockTest.class,
                 conformance2.naming.QueueTest.class,
                 conformance2.naming.ReplicationTest.class,
                 conformance2.naming.DeletionTest.class
            };
        Series                      series = new Series(tests);
        SeriesReport                report = series.run(3, System.out);

        // Print the report and exit with an appropriate exit status.
        report.print(System.out);
        System.exit(report.successful() ? 0 : 2);
    }
}
