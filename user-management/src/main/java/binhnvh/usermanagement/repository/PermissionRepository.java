package binhnvh.usermanagement.repository;

import binhnvh.usermanagement.entity.Permission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.Set;

@Repository
public interface PermissionRepository extends JpaRepository<Permission, Long> {

    Optional<Permission> findByName(String name);

    Set<Permission> findByNameIn(Set<String> names);

    boolean existsByName(String name);
}