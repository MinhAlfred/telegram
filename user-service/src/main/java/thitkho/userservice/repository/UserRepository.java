package thitkho.userservice.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import thitkho.userservice.model.User;
import thitkho.userservice.model.enums.AuthProvider;
import thitkho.userservice.model.enums.Role;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, String> {

    // Auth
    Optional<User> findByEmail(String email);
    Optional<User> findByUsername(String username);
    Optional<User> findByProviderIdAndProvider(String providerId, AuthProvider provider);

    boolean existsByEmail(String email);
    boolean existsByUsername(String username);

    // Online status
    @Query("SELECT u FROM User u WHERE u.id IN :ids AND u.onlineStatus = 'ONLINE'")
    List<User> findOnlineUsersByIds(@Param("ids") List<String> ids);

    // Admin
    Page<User> findByRole(Role role, Pageable pageable);

    @Query("SELECT u FROM User u WHERE LOWER(u.username) LIKE LOWER(CONCAT('%', :query, '%')) " +
            "OR LOWER(u.displayName) LIKE LOWER(CONCAT('%', :query, '%'))")
    Page<User> searchUsers(@Param("query") String query, Pageable pageable);
}