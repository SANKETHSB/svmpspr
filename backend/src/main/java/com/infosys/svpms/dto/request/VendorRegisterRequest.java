package com.infosys.svpms.dto.request;
import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class VendorRegisterRequest {
    @NotBlank @Size(max=200) public String companyName;
    @NotBlank @Email public String email;
    @NotBlank @Size(min=8) public String password;
    @NotBlank @Size(min=15, max=15, message="GST must be 15 characters") public String gstNumber;
    @NotBlank public String registrationId;
    @Size(max=15) public String phone;
    @Size(max=500) public String address;
    public String contactPerson;
}
