package net.playlegend.groupmanager.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.Objects;
import java.util.Set;

@Entity
@Table(name = "permissions")
public class Permission {

  @Id
  @Column(name = "permission", nullable = false)
  @Getter
  @Setter
  private String permission;

  @ManyToMany(mappedBy = "permissions")
  @Getter
  @Setter
  private Set<Group> groups;

  @Override
  public boolean equals(Object o) {
    if (o instanceof Permission) {
      return this.permission.equalsIgnoreCase(((Permission) o).getPermission());
    }
    if (o instanceof String) {
      return this.permission.equalsIgnoreCase((String) o);
    }
    return super.equals(o);
  }

  @Override
  public int hashCode() {
    return Objects.hash(permission);
  }
}
