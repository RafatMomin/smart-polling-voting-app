package onetoone.Location.controller;


import onetoone.Location.dto.EligibilityRequest;
import onetoone.Location.dto.LocationRequest;
import onetoone.Location.service.LocationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/location")
public class LocationController {

    @Autowired
    private LocationService locationService;

    // CHECK if the users location is in the campus geofence (eligible)
    @PostMapping("/eligibility")
    public EligibilityRequest checkEligibility(@RequestBody LocationRequest location) {
        return locationService.checkEligibility(location.getLat(), location.getLng());
    }
}
