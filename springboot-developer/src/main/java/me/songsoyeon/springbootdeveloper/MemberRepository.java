package me.songsoyeon.springbootdeveloper;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MemberRepository extends JpaRepository<Member, Long> {
    Optional<Member> findByName(String name);

// 너무 복잡한 쿼리이거나 성능이 중요해서 SQL 쿼리를 직접 사용해야 하는 경우
    @Query("select m from Member m where m.name = ?1")
    Optional<Member> findByNameQuery(String name);
}
