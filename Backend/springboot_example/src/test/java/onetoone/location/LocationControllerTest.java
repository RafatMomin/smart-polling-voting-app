package onetoone.location;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import onetoone.Location.controller.LocationController;
import onetoone.Location.dto.EligibilityRequest;
import onetoone.Location.dto.LocationRequest;
import onetoone.Location.service.LocationService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class LocationControllerTest {

    @Mock
    private LocationService locationService;

    @InjectMocks
    private LocationController controller;

    /*
     * LOCATION VERIFICATION FEATURE TEST: checkEligibility delegates to LocationService.
     * verifies that the exact lat/lng is forwarded.
     */
    @Test
    public void checkEligibility_delegatesToService() {
        LocationRequest req = new LocationRequest(42.0, -93.0);
        EligibilityRequest expected = new EligibilityRequest(true, 10.0, "rule");
        when(locationService.checkEligibility(42.0, -93.0)).thenReturn(expected);

        EligibilityRequest result = controller.checkEligibility(req);

        assertSame(expected, result);
        verify(locationService).checkEligibility(42.0, -93.0);
    }
}