package com.atmosware.library_project.core.utilities.mapping;

import com.atmosware.library_project.business.dtos.RegisterRequest;
import com.atmosware.library_project.business.dtos.UserResponse;
import com.atmosware.library_project.entities.User;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper
public interface UserMapper {

    UserMapper INSTANCE = Mappers.getMapper(UserMapper.class);

    UserResponse mapToResponse(User user);
    List<UserResponse> mapToResponseList(List<User> users);
    User mapRequestToEntity(RegisterRequest registerRequest);
    User mapResponseToEntity(UserResponse userResponse);
}
