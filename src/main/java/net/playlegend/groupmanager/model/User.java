package net.playlegend.groupmanager.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "users")
public class User {

  @Id
  @Column(name = "uuid", nullable = false)
  @Getter
  @Setter
  private UUID uuid;

  @Column(name = "name")
  @Getter
  @Setter
  private String name;

  @ManyToOne
  @JoinColumn(name = "groupId")
  @Getter
  @Setter
  private Group group;

  @OneToMany(mappedBy = "user", cascade = CascadeType.REMOVE, orphanRemoval = true)
  @Getter
  @Setter
  private Set<RankSign> rankSigns;

  @Column(name = "groupValidUntil")
  @Getter
  @Setter
  private long groupValidUntil = -1;
}