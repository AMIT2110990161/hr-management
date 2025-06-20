package com.hr_management.hr_management.controller;

import com.hr_management.hr_management.exception.ResourceNotFoundException;
import com.hr_management.hr_management.mapper.LocationMapper;
import com.hr_management.hr_management.model.dto.ApiResponseDto;
import com.hr_management.hr_management.model.dto.LocationDTO;
import com.hr_management.hr_management.model.dto.PostLocationDTO;
import com.hr_management.hr_management.model.entity.Country;
import com.hr_management.hr_management.model.entity.Location;
import com.hr_management.hr_management.repository.CountryRepository;
import com.hr_management.hr_management.repository.LocationRepository;
import com.hr_management.hr_management.repository.RegionRepository;
import com.hr_management.hr_management.utils.BuildResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/locations")  // Base path for location-related endpoints
public class LocationsController {

    @Autowired
    private LocationRepository locationRepository;

    @Autowired
    private LocationMapper locationMapper;

    @Autowired
    private CountryRepository countryRepository;

    @Autowired
    private RegionRepository regionRepository;

    @GetMapping
    public ResponseEntity<ApiResponseDto> findAllLocations(HttpServletRequest request) {
        List<LocationDTO> locationDTOs = locationRepository.findAll().stream()
                .map(locationMapper::toLocationDto)
                .collect(Collectors.toList());

        return BuildResponse.success(locationDTOs, "List of all Locations", request.getRequestURI());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponseDto> findLocationById(@PathVariable BigDecimal id, HttpServletRequest request) {
        Location location = locationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Location with ID " + id + " does not exist"));

        LocationDTO locationDTO = locationMapper.toLocationDto(location);

        return BuildResponse.success(locationDTO, "Location fetched successfully", request.getRequestURI());
    }

    @PostMapping
    public ResponseEntity<ApiResponseDto> createLocation(@RequestBody PostLocationDTO postLocationDTO, HttpServletRequest request) {

        Country inputCountry = postLocationDTO.getCountry();

        // Check if country exists
        Country country = countryRepository.findById(inputCountry.getCountryId())
                .orElseGet(() -> {
                    // Check if region exists
                    if (!inputCountry.getRegion().getRegionName().isEmpty()) {
                        regionRepository.findById(inputCountry.getRegion().getRegionId())
                                .orElseGet(() -> regionRepository.save(inputCountry.getRegion()));
                    }
                    return countryRepository.save(inputCountry);
                });

        Location location = locationMapper.toLocationEntity(postLocationDTO, country);
        Location savedLocation = locationRepository.save(location);
        LocationDTO locationDTO = locationMapper.toLocationDto(savedLocation);

        return BuildResponse.success(locationDTO, "Location created successfully", request.getRequestURI());
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponseDto> updateLocation(@PathVariable BigDecimal id,
                                                         @RequestBody PostLocationDTO postLocationDTO,
                                                         HttpServletRequest request) {

        Location existingLocation = locationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Location with ID " + id + " not found"));


        Country country = countryRepository.findById(postLocationDTO.getCountry().getCountryId())
                .orElseGet(() -> countryRepository.save(postLocationDTO.getCountry()));


        existingLocation.setStreetAddress(postLocationDTO.getStreetAddress());
        existingLocation.setPostalCode(postLocationDTO.getPostalCode());
        existingLocation.setCity(postLocationDTO.getCity());
        existingLocation.setStateProvince(postLocationDTO.getStateProvince());
        existingLocation.setCountry(country);


        Location updatedLocation = locationRepository.save(existingLocation);
        LocationDTO locationDTO = locationMapper.toLocationDto(updatedLocation);

        return BuildResponse.success(locationDTO, "Location updated successfully", request.getRequestURI());
    }




    @GetMapping("/byCountry/{countryId}")
    public ResponseEntity<ApiResponseDto> getLocationsByCountry(@PathVariable String countryId, HttpServletRequest request) {
        List<LocationDTO> locationDTOs = locationRepository.findByCountryCountryId(countryId).stream()
                .map(locationMapper::toLocationDto)
                .collect(Collectors.toList());

        return BuildResponse.success(locationDTOs, "Locations in country " + countryId, request.getRequestURI());
    }

    @GetMapping("/byState/{state}")
    public ResponseEntity<ApiResponseDto> getLocationsByState(@PathVariable String state, HttpServletRequest request) {
        List<Location> locations = locationRepository.findByStateProvinceIgnoreCase(state);

        if (locations.isEmpty()) {
            throw new ResourceNotFoundException("No locations found in state/province: " + state);
        }

        List<LocationDTO> locationDTOs = locations.stream()
                .map(locationMapper::toLocationDto)
                .collect(Collectors.toList());

        return BuildResponse.success(locationDTOs,
                "List of locations in state/province: " + state,
                request.getRequestURI());
    }


}
