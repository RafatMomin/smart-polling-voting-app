package onetoone.location;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import onetoone.Location.dto.EligibilityRequest;
import onetoone.Location.service.CampusConfig;
import onetoone.Location.service.LocationService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class LocationServiceTest {

    @Mock
    private CampusConfig campusConfig;

    @InjectMocks
    private LocationService locationService;

    private CampusConfig.Center center;
    private CampusConfig.Radius radius;

    @Before
    public void setup() {
        center = new CampusConfig.Center();
        center.setLatitude(42.0267);
        center.setLongitude(-93.6465);

        radius = new CampusConfig.Radius();
        radius.setMeters(500.0);

        when(campusConfig.getCenter()).thenReturn(center);
        when(campusConfig.getRadius()).thenReturn(radius);
    }

    /*
     * LOCATION VERIFICATION FEATURE TEST: calculateDistance
     * same coordinates should give ~0 distance, and a small difference should be > 0.
     */
    @Test
    public void calculateDistance_samePoint_isZero() {
        double d = locationService.calculateDistance(42.0, -93.0, 42.0, -93.0);
        assertEquals(0.0, d, 0.5);
    }

    /*
     * LOCATION VERIFICATION FEATURE TEST: eligibility inside campus radius.
     * point close to campus center should be eligible.
     */
    @Test
    public void checkEligibility_insideRadius_isEligible() {
        EligibilityRequest res = locationService.checkEligibility(42.0268, -93.6466);

        assertTrue(res.isEligible());
        assertEquals("within 500 m of ISU campus center", res.getRule());
        assertNotNull(res.getServerTime());
    }

    /*
     * LOCATION VERIFICATION FEATURE TEST: eligibility outside campus radius.
     * far coordinate should be considered not eligible.
     */
    @Test
    public void checkEligibility_outsideRadius_notEligible() {
        EligibilityRequest res = locationService.checkEligibility(41.0, -93.0);

        assertFalse(res.isEligible());
    }

    /*
     * LOCATION VERIFICATION FEATURE TEST: validateLocation throws if outside geofence.
     * verify it throws RuntimeException when not eligible.
     */
    @Test(expected = RuntimeException.class)
    public void validateLocation_outsideGeofence_throwsException() {
        radius.setMeters(10.0); // tiny radius

        locationService.validateLocation(41.0, -93.0);
    }
}
