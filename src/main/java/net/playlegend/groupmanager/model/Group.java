package net.playlegend.groupmanager.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

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
}
