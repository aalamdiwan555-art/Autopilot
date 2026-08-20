import { Router, type IRouter } from "express";
import healthRouter from "./health";
import mobileRouter from "./mobile";

const router: IRouter = Router();

router.use(healthRouter);
router.use(mobileRouter);

export default router;
