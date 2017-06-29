% is_same(A,B):-A = B.
% is_not_same(A,B):-A \= B.

parent(A,B):-mother(A,B).
parent(A,B):-father(A,B).

child(A,B):-parent(B,A).

grandparent(A,B):-parent(A,C),parent(C,B).

great_grandparent(A,B):-parent(A,C),grandparent(C,B).
son(A,B):-child(A,B),male(A).
daughter(A,B):-child(A,B),female(A).
grand_father(A,B):-grandparent(A,B),male(A).
grand_mother(A,B):-grandparent(A,B),female(A).
husband(A,B):-father(A,C),mother(B,C).
wife(A,B):-mother(A,C),father(B,C).

% brother_of_including_self(A,B):-son(A,C),mother(C,B).
% brother(A,B):-brother_of_including_self(A,B),is_not_same(A,B).
brother(A,B):-son(A,C),mother(C,B).

% sister_of_including_self(A,B):-daughter(A,C),mother(C,B).
% sister(A,B):-sister_of_including_self(A,B),is_not_same(A,B).
sister(A,B):-daughter(A,C),mother(C,B).

uncle_paternal(A,B):-brother(A,C),father(C,B).
father_of_mother(A,B):-father(A,C),mother(C,B).
aunt_paternal(A,B):-sister(A,C),father(C,B).
mother_of_mother(A,B):-mother(A,C),mother(C,B).
uncle_maternal_maamaa(A,B):-brother(A,C),mother(C,B).
wife_of_maamaa(A,B):-wife(A,C),uncle_maternal_maamaa(C,B).
son_of_maamaa(A,B):-son(A,C),uncle_maternal_maamaa(C,B).
daughter_of_maamaa(A,B):-daughter(A,C),uncle_maternal_maamaa(C,B).
husband_of_daughter(A,B):-husband(A,C),daughter(C,B).
father_of_wife(A,B):-father(A,C),wife(C,B).
mother_of_wife(A,B):-mother(A,C),wife(C,B).
sons_wife(A,B):-wife(A,C),son(C,B).
father_of_sons_wife(A,B):-father(A,C),sons_wife(C,B).
mother_of_sons_wife(A,B):-mother(A,C),sons_wife(C,B).
daughters_husband(A,B):-husband(A,C),daughter(C,B).
father_of_daughters_husband(A,B):-father(A,C),daughters_husband(C,B).
mother_of_daughters_husband(A,B):-mother(A,C),daughters_husband(C,B).
fathers_sister_buwa(A,B):-sister(A,C),father(C,B).
husband_of_buwa(A,B):-husband(A,C),fathers_sister_buwa(C,B).
daughter_of_buwa(A,B):-daughter(A,C),fathers_sister_buwa(C,B).
son_of_buwa(A,B):-son(A,C),fathers_sister_buwa(C,B).
brothers_sister(A,B):-sister(A,C),brother(C,B).
brothers_sisters_son(A,B):-son(A,C),brothers_sister(C,B).
brothers_brother(A,B):-brother(A,C),brother(C,B).
brothers_brothers_son(A,B):-son(A,C),brothers_brother(C,B).
brothers_sisters_daughter(A,B):-daughter(A,C),brothers_sister(C,B).
brothers_brothers_daughter(A,B):-daughter(A,C),brothers_brother(C,B).
sisters_sister(A,B):-sister(A,C),sister(C,B).
sisters_sisters_son(A,B):-son(A,C),sisters_sister(C,B).
sisters_sisters_daughter(A,B):-daughter(A,C),sisters_sister(C,B).
sisters_brother(A,B):-brother(A,C),sister(C,B).
sisters_brothers_son(A,B):-son(A,C),sisters_brother(C,B).
sisters_brothers_daughter(A,B):-daughter(A,C),sisters_brother(C,B).