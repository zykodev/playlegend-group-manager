package net.playlegend.groupmanager.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

import java.util.UUID;

@Entity
@Table(name = "ranksigns")
public class RankSign {

  @Id
  @Column(name = "id", nullable = false)
  @Getter
  @GeneratedValue
  @UuidGenerator
  private UUID id;

  @Column(name = "px")
  @Getter
  @Setter
  private double posX;

  @Column(name = "py")
  @Getter
  @Setter
  private double posY;

  @Column(name = "pz")
  @Getter
  @Setter
  private double posZ;

  @Column(name = "world")
  @Getter
  @Setter
  private String world;

  @ManyToOne
  @JoinColumn(name = "user_id")
  @Getter
  @Setter
  private User user;
}
