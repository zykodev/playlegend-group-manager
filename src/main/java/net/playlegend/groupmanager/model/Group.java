package net.playlegend.groupmanager.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

import java.util.HashSet;
import java.util.Objects;
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

  @OneToMany(mappedBy = "group", fetch = FetchType.EAGER)
  @Getter
  @Setter
  private Set<User> users = new HashSet<>();

  @ManyToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
  @JoinTable(
      name = "groups_permissions",
      inverseJoinColumns = {@JoinColumn(name = "permissionId")},
      joinColumns = {@JoinColumn(name = "groupId")})
  @Getter
  @Setter
  private Set<Permission> permissions = new HashSet<>();

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof Group) {
      return this.getId().equals(((Group) obj).getId());
    }
    return super.equals(obj);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id);
  }
}
