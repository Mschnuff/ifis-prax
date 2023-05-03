package com.analysetool.repositories;

import com.analysetool.modells.WPTerm;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WPTermRepository extends JpaRepository<WPTerm, Long> {

    // benutzerdefinierte Methoden, falls benötigt


}

