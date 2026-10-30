package com.hotelbooking.booking.application.mapper;

import com.hotelbooking.booking.application.dto.response.UserResponse;
import com.hotelbooking.booking.domain.entity.User;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-10-30T14:20:05+0300",
    comments = "version: 1.6.3, compiler: Eclipse JDT (IDE) 3.46.200.v20261008-1115, environment: Java 25.0.4.1 (Eclipse Adoptium)"
)
@Component
public class UserMapperImpl implements UserMapper {

    @Override
    public UserResponse toResponse(User user) {
        if ( user == null ) {
            return null;
        }

        Long id = null;
        String username = null;

        id = user.getId();
        username = user.getUsername();

        String role = user.getRole().name();

        UserResponse userResponse = new UserResponse( id, username, role );

        return userResponse;
    }
}
