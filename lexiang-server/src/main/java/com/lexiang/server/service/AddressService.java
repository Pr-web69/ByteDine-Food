package com.lexiang.server.service;

import com.lexiang.server.dto.AddressDTO;
import com.lexiang.server.entity.Address;
import java.util.List;

public interface AddressService {

    List<Address> list();
    Address getDefault();
    void add(AddressDTO dto);
    void update(Long id, AddressDTO dto);
    void delete(Long id);
    void setDefault(Long id);
}