-- GGSIPU grade scale (verify against your batch's ordinance before relying on it).
-- Boundaries are inclusive on both ends.
DELETE FROM grade_scale;
INSERT INTO grade_scale (grade, min_marks, max_marks, grade_point) VALUES
  ('O',  90, 100, 10),
  ('A+', 75,  89,  9),
  ('A',  65,  74,  8),
  ('B+', 55,  64,  7),
  ('B',  50,  54,  6),
  ('C',  45,  49,  5),
  ('P',  40,  44,  4),
  ('F',   0,  39,  0);
