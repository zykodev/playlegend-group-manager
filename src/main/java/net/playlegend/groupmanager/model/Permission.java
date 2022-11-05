package net.playlegend.groupmanager.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

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
}
