import test from "node:test";
import assert from "node:assert/strict";
import { display, human, money } from "./format.js";
test("empty grades remain distinct from zero", () => {
  assert.equal(display(null), "—");
  assert.equal(display(0), "0");
});
test("roles and currencies are readable", () => {
  assert.equal(human("SCHOOL_STAFF"), "School Staff");
  assert.match(money(12.5, "USD"), /12\.50/);
});
