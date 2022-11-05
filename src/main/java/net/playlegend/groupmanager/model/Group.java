package net.playlegend.groupmanager.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "groups")
public class Group {

  @Id
  @Column(name = "id", nullable = false)
  @Getter
  @GeneratedValue
  @UuidGenerator
  private UUID id;

  @Column(name = "name", nullable = false, unique = true)
  @Getter
  @Setter
  private String name;

  @Column(name = "priority")
  @Getter
  @Setter
  private Integer priority = 99;

  @Column(name = "prefix")
  @Getter
  @Setter
  private String prefix = "";

  @OneToMany(mappedBy = "group", cascade = CascadeType.ALL)
  @Getter
  @Setter
  private Set<User> users = new HashSet<>();

  @ManyToMany(cascade = CascadeType.ALL)
  @JoinTable(
      name = "groups_permissions",
      inverseJoinColumns = {@JoinColumn(name = "permissionId")},
      joinColumns = {@JoinColumn(name = "groupId")})
  @Getter
  @Setter
  private Set<Permission> permissions = new HashSet<>();
}
